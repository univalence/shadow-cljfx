(ns article.ex-02
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as fx.ext.web-view]))

(def html-content
  "<!DOCTYPE html><html><body><h1> Je suis un H1 </h1></body></html>")

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type fx.ext.web-view/with-engine-props
                  :desc    {:fx/type :web-view}
                  :props   {:content html-content}}}}))