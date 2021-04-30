(ns chaussette.server
  (:require
   [compojure.core :as compojure :refer [GET]]
   [ring.middleware.params :as params]
   [compojure.route :as route]
   [aleph.http :as http]
   [manifold.stream :as s]
   [manifold.deferred :as d]))


;; this is holding connections to web-views
(def connections (atom {}))


(defn make-handler
  "build a web-socket handler from the given function `f`
   that will be called on every received messages."
  [f]
  (fn [req]
    (d/let-flow [conn (http/websocket-connection req)
                 ;; the first message received by this websocket is the id of the client
                 id-str (s/take! conn)]
                (let [id (read-string id-str)]
                  ;; we put the new connection into our connections atom
                  (swap! connections assoc id conn)
                  ;; then apply f to subsequent messages
                  (s/consume (partial f id) conn)))))


(def logging-handler
  "An handler that logs what it receives"
  (make-handler #(println %1 "sent: " %2)))


(def handler
  (params/wrap-params
   (compojure/routes
    (GET "/ws" [] logging-handler)
    (route/resources "/")
    (route/not-found "No such page."))))


(defn start! [port]
  (http/start-server handler {:port port}))


(defn close! [server]
  (.close server))


(defn send! [id message]
  (when-let [conn (get @connections id)]
    (s/put! conn (pr-str message))))



(comment

 (handler {:uri "/index.html" :request-method :get})
 (def S (start!))
 (close! S)
 (get @connections :chaussette.view1)
 (send! :shadow.cljfx/test-one "hi from server")

 )


