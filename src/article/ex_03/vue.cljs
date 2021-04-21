(ns article.ex-03.vue
  (:require [reagent.dom :as rd]))

(def markup
  [:h1 "Hello shadow-cljx"])

(defn ^:export init []
  (rd/render markup
             (.getElementById js/document "app")))