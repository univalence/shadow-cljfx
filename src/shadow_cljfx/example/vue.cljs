(ns shadow-cljfx.example.vue
  (:require [reagent.dom :as rd]
            [reagent.core :as r]))

(def state (r/atom {:message "Hello shadow-cljx"}))

(defn root []
  [:div#app
   [:h1 (:message @state)]
   [:button {:on-click (fn [_] (js/window.app.send "toc toc"))}
    "knockin on server's door"]])

(defn ^:export init []

  (set! js/window.webView
        (js-obj "send"
                (fn [data] (swap! state assoc :message data))))

  (rd/render [root]
             (.getElementById js/document "app")))
