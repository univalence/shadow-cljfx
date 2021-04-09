(ns app.server
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

(def client-conn (atom nil))

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



(defn logging-handler
  [req]
  (-> (d/let-flow [conn (http/websocket-connection req)]
                  (reset! client-conn conn)
                  (s/consume
                   #(println "server receives: " %)
                   conn))
      (d/catch
       (fn [_]
         non-websocket-request))))

(defn logging-handler2
  [req]
  (-> (d/let-flow [conn (http/websocket-connection req)]
                  (if-not conn
                    non-websocket-request
                    (d/let-flow [id-str (s/take! conn)]
                                (let [id (read-string id-str)]
                                  (swap! connections assoc id conn)
                                  (s/consume
                                   #(println id "sent: " %)
                                   conn)))))
      (d/catch
       (fn [_]
         non-websocket-request))))


(def handler
  (params/wrap-params
   (compojure/routes
    (GET "/echo" [] echo-handler)
    (GET "/talk" [] logging-handler2)
    (route/not-found "No such page."))))

(def ws-server
  (http/start-server handler {:port 3000}))

(defn send!_old [message]
  (s/put! @client-conn message))

(defn send! [id message]
  (s/put! (get @connections id) message))


(comment
 (.close ws-server)

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
        doall))) ;=> ("0" "1" "2" "3" "4" "5" "6" "7" "8" "9")