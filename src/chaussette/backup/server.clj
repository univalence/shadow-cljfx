(ns chaussette.server
  (:require
   [compojure.core :as compojure :refer [GET]]
   [ring.middleware.params :as params]
   [compojure.route :as route]
   [aleph.http :as http]
   [byte-streams :as bs]
   [manifold.stream :as s]
   [manifold.deferred :as d]
   [manifold.bus :as bus]
   [clojure.core.async :as a]))


;; this is holding connections to web-views
(def connections (atom {}))


(def non-websocket-request
  {:status  400
   :headers {"content-type" "application/text"}
   :body    "Expected a websocket request."})


(defn echo-handler
  [req]
  (->
   (d/let-flow [socket (http/websocket-connection req)]
               (clojure.pprint/pprint [:echo-socket req])
               (s/connect socket socket))
   (d/catch
    (fn [_]
      non-websocket-request))))


(defn make-handler [consume]
  (fn [req]
    (-> (d/let-flow [conn (http/websocket-connection req)]
                    (if-not conn
                      non-websocket-request
                      (d/let-flow [id-str (s/take! conn)]
                                  (let [id (read-string id-str)]
                                    (swap! connections assoc id conn)
                                    (s/consume (partial consume id) conn)))))
        (d/catch
         (fn [_]
           non-websocket-request)))))


(def logging-handler
  (make-handler #(println %1 "sent: " %2)))


(def handler
  (params/wrap-params
   (compojure/routes
    (GET "/echo" [] echo-handler)
    (GET "/talk" [] logging-handler)
    (route/resources "/")
    (route/not-found "No such page."))))


(def ws-server
  (http/start-server handler
                     {:port 3000}))


(defn send! [id message]
  (when-let [conn (get @connections id)]
    (s/put! conn message)))



(comment

 #_[bidi.ring :as bring]

 (def routes
   ["" [["/echo" echo-handler]
        ["/talk" logging-handler]
        ["/" (bring/resources {:prefix "public/"})]]])

 (def handler2 (bring/make-handler routes))


 (handler {:uri "/index.html" :request-method :get})
 (.close ws-server)

 (resource-response "")

 (do @connections)
 (get @connections :shadow.cljfx/test-one)
 (send! :shadow.cljfx/test-one "hi from server")

 (let [conn @(http/websocket-client "ws://localhost:3000/talk")]
   (s/put! (s/->source conn) "hello from server"))


 (let [conn @(http/websocket-client "ws://localhost:3000/echo")]

   (s/put-all! conn
               (->> 10 range (map str)))

   (->> conn
        (s/transform (take 10))
        s/stream->seq
        doall))
 )


