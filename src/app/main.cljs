(ns app.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def WEBSOCKET "ws://localhost:3000/")

(defn try-websocket! []
  (go (let [stream (<! (ws/connect (str WEBSOCKET "echo")))]
        (>! (:sink stream) "Hello World")
        (js/console.log (<! (:source stream)))
        (ws/close stream))))

(def ws-conn (atom nil))

(defn init-websocket! [handle]
  (letfn [(listen []
            (go-loop []
                     (if-let [source (:source @ws-conn)]
                       (handle (<! source)))
                     (recur)))]
    (go
     ;; connection
     (reset! ws-conn (<! (ws/connect (str WEBSOCKET "talk"))))
     ;; give the connection id to server
     (>! (:sink @ws-conn) :shadow.cljfx/test-one)
     ;; listening to incoming messages
     (listen))))

(defn ^:dev/after-load reload! []
  (rd/render [:h1 "Hello shadow-cljfx !"
              [:button {:on-click (fn [] (go (>! (:sink @ws-conn) "hello from client")))}
               "say hello to server"]]
             (.getElementById js/document "app")))

(defn ^:export init []
  (try-websocket!)
  (init-websocket! (fn [message] (println "ws receives: " message)))
  (reload!))



