(ns components.one.core (:require [reagent.dom :as dom]))

(defn main [] (dom/render [:div "Hello"] (js/document.getElementById "app")))