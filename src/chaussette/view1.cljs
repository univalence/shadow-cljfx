(ns chaussette.view1
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [cljs.reader :as reader])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


(def state
  (atom {:conn   nil
         :id     :chaussette.view1
         :ws-url "ws://localhost:3000/ws"}))


(def reactive-state
  (r/atom {:title    "View1"
           :messages []}))


(defn init-websocket! [handle]

  (let [{:keys [id ws-url]} @state
        listen #(go-loop []
                         (if-let [source (get-in @state [:conn :source])]
                           (do (handle (reader/read-string (<! source))) (recur))
                           (println "something went wrong establishing websocket connection")))]
    (go
     (let [conn (<! (ws/connect ws-url))]
       ;; connection
       (swap! state assoc :conn conn)
       ;; give the connection id to server
       (>! (:sink conn) id)
       ;; listening to incoming messages
       (listen)))))


(defn send! [message]
  (go (>! (get-in @state [:conn :sink])
          (pr-str message))))


(defn root []
  [:div#app
   [:h1 (:title @reactive-state)]
   [:button {:on-click #(send! "hello from client")} "say hello to server !"]
   [:h3 "Messages: "]
   (into [:div#messages]
         (map (fn [m] [:div.message (str m)])
              (:messages @reactive-state)))])


(defn handler [message]
  (println "received: " message)
  (swap! reactive-state update :messages
         conj (reader/read-string message)))


(defn ^:dev/after-load reload! []
  (init-websocket! handler)
  (rd/render [root] (.getElementById js/document "app")))


(defn ^:export init []
  (reload!))



