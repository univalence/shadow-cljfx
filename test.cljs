(ns foo.bar (:require [reagent.core :as r] [reagent.dom :as dom]))

(defn #:dev{:after-load true} render [] (dom/render [:div "Hi"] (js/document.getElementById "app")))

(defn {:export true} main [] (render))