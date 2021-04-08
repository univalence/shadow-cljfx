(ns app.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn ^:dev/after-load reload! []
  (rd/render [:h1 "yop reagent !!"]
             (.getElementById js/document "app")))

(defn ^:export init []
  (reload!))



