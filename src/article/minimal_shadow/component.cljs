(ns component
  (:require [reagent.dom :as rd]
            [reagent.core :as r]))


(defn root
  []
  [:div [:h1 "Hi"]])


(defn render
  {:dev/after-load true}
  []
  (rd/render [root] (js/document.getElementById "app")))


(defn init
  {:export true}
  []
  (render))
