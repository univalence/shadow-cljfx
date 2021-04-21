(ns foo.bar
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [cljs.reader :as reader])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def WEBSOCKET "ws://localhost:3000/")

(def ID :foo.bar)

(def state (r/atom {}))

(def ws-conn (atom nil))

(defn init-websocket!
  [id handle]
  (letfn [(listen
            []
            (go-loop
              []
              (if-let [source (:source (clojure.core/deref ws-conn))]
                (do (handle (reader/read-string (<! source))) (recur))
                (println
                  "something went wrong establishing websocket connection"))))]
    (go (reset! ws-conn (<! (ws/connect (str WEBSOCKET "talk"))))
        (>! (:sink (clojure.core/deref ws-conn)) [ID id])
        (listen))))

(defn render
  {:dev/after-load true}
  []
  (rd/render
    [:h1 "Hello shadow-cljfx !"
     [:button
      {:on-click (fn []
                   (go (>! (:sink (clojure.core/deref ws-conn))
                           "hello from client")))} "say hello to server"]]
    (.getElementById js/document "app")))

(defn main
  {:export true}
  [& [id]]
  (init-websocket! id (fn [message] (println "ws receives: " message)))
  (render))