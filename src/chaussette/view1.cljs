(ns chaussette.view1
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [haslett.client :as ws]
            [cljs.reader :as reader]
            [cljs.core.async
             :refer [<! >!]
             :refer-macros [go go-loop]]))

(def WS_ENDPOINT "ws://localhost:3000/ws")

(def VIEW_ID :chaussette.view1)

(def *conn (atom nil))

;; socket -----------------------------------------------------------------------


(defn init-websocket!
  [handle]
  (go
   (let [;; connecting
         {:as conn :keys [sink source]} (<! (ws/connect WS_ENDPOINT))]
     ;; persist connection
     (reset! *conn conn)
     ;; give the connection id to server
     (>! sink VIEW_ID)
     ;; listening to incoming messages
     (loop []
       (handle (reader/read-string (<! source)))
       (recur)))))


(defn send! [message]
  (go (>! (get @*conn :sink)
          (pr-str message))))

;; view -----------------------------------------------------------------------


(def reactive-state
  (r/atom {:title    "View1"
           :messages []}))


(defn root []
  [:div#app
   [:h1 (:title @reactive-state)]
   [:button {:on-click #(send! "hello from client")} "say hello to server !"]
   [:h3 "Messages : "]
   (into [:div#messages]
         (map (fn [m] [:div.message (str m)])
              (:messages @reactive-state)))])


(defn handler [message]
  (println "received: " message)
  (swap! reactive-state update :messages
         conj message))

;; api -----------------------------------------------------------------------

(defn ^:dev/after-load reload! []
  (init-websocket! handler)
  (rd/render [root] (.getElementById js/document "app")))


(defn ^:export init []
  (reload!))



