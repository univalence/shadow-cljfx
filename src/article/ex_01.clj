(ns article.ex-01
  (:require [cljfx.api :as fx]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :label
                  :text " Bonjour !"}}}))