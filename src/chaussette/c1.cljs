(ns chaussette.c1
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [cljs.reader :as reader])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def WEBSOCKET "ws://localhost:3000/")

(def ID :shadow.cljfx/c1)

(def state (r/atom {}))

(defn try-websocket! []
  (go (let [stream (<! (ws/connect (str WEBSOCKET "echo")))]
        (>! (:sink stream) "Hello World")
        (js/console.log (<! (:source stream)))
        (ws/close stream))))

(def ws-conn (atom nil))

(defn init-websocket! [id handle]
  (letfn [(listen []
            (go-loop []
                     (if-let [source (:source @ws-conn)]
                       (do (handle (reader/read-string (<! source))) (recur))
                       (println "something went wrong establishing websocket connection"))))]
    (go
     ;; connection
     (reset! ws-conn (<! (ws/connect (str WEBSOCKET "talk"))))
     ;; give the connection id to server
     (>! (:sink @ws-conn) [ID id])
     ;; listening to incoming messages
     (listen))))

(defn ^:dev/after-load reload! []
  (rd/render [:h1 "Hello shadow-cljfx !"
              [:button {:on-click (fn [] (go (>! (:sink @ws-conn) "hello from client")))}
               "say hello to server"]]
             (.getElementById js/document "app")))

(defn ^:export init [& [id]]
  (try-websocket!)
  (init-websocket! id (fn [message] (println "ws receives: " message)))
  (reload!))



