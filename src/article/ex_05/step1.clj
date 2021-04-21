(ns article.ex-05.step1
  (:require
   [cljfx.api :as fx]
   [cljfx.prop :as prop]
   [cljfx.mutator :as mutator]
   [cljfx.lifecycle :as lifecycle])
  (:import (javafx.scene.web WebView)))

(defprotocol ISend
  (send [this message]))

(defn sender
  "turn the given function into an instance of ISend"
  [f]
  (reify ISend
    (send [_ message] (f message))))

(def engine-ext

  (fx/make-ext-with-props

   {:html    (prop/make
              (mutator/setter
               #(.loadContent (.getEngine ^WebView %1) %2 "text/html"))
              lifecycle/scalar)

    :handler (prop/make
              (mutator/setter
               (fn [this f]
                 (let [engine (.getEngine ^WebView this)
                       window (.executeScript engine "window")]
                   (.setMember window "app" (sender f)))))
              lifecycle/scalar)

    }))

(def html-content
  "<!DOCTYPE html><html>
    <body>
      <div id=\"app\"> Hello </h1>
      <button onclick=\"app.send('anyone here ?')\">anyone here ?</button>
    </body>
   </html>")

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type engine-ext
                       :desc    {:fx/type :web-view}
                       :props   {:html    html-content
                                 :handler (fn [data] (println "Client sent: " data))}}}}))