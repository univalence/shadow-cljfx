(ns article.ex-04
  (:require [cljfx.api :as fx]
            [cljfx.prop :as prop]
            [cljfx.mutator :as mutator]
            [cljfx.lifecycle :as lifecycle]
            [hiccup.page :refer [html5]]
            [cljfx.coerce :as coerce])
  (:import (javafx.beans.value ChangeListener ObservableValue)
           (javafx.concurrent Worker$State)
           (javafx.scene.web WebView)))


(def ext-with-web-engine
  (fx/make-ext-with-props

   {:html     (prop/make
               (mutator/setter
                #(.loadContent (.getEngine ^WebView %1) %2 "text/html"))
               lifecycle/scalar)

    :bridge   (prop/make
               (mutator/setter
                (fn [this bridge]
                  (let [engine (.getEngine ^WebView this)
                        window (.executeScript engine "window")]
                    (.setMember window "bridge" bridge))))
               lifecycle/scalar)

    :on-error (prop/make
               (mutator/setter
                #(.setOnError (.getEngine ^WebView %1) %2))
               lifecycle/event-handler
               :coerce coerce/event-handler)

    :on-load  (prop/make
               (mutator/setter
                (fn [this f]
                  (let [engine (.getEngine ^WebView this)]
                    (.addListener (.stateProperty (.getLoadWorker engine))
                                  (proxy [ChangeListener] []
                                    (changed [^ObservableValue ov
                                              ^Worker$State old-state
                                              ^Worker$State new-state]
                                      (if (= new-state Worker$State/SUCCEEDED)
                                        (f engine (.getDocument engine)))))))))
               lifecycle/scalar)}))

(def html-content
  "<!DOCTYPE html><html>
    <body>
      <div id=\"app\"> Hello </h1>
      <button onclick=\"bridge.toJava('alors cette prostate ?')\">send</button>
    </body>
   </html>")

(declare send!)

(defprotocol IWebBridge
  (toJava [_ data]))

(def bridge
  (reify IWebBridge
    (toJava [_ data] (println "receive " data " from client."))))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type ext-with-web-engine
                       :desc    {:fx/type :web-view}
                       :props   {:html     html-content
                                 :bridge   bridge
                                 :on-error (fn [e] (println "error: " e))
                                 :on-load  (fn [engine _]
                                             (.executeScript engine "fromJava = function(x){document.getElementById(\"app\").innerHTML = \"toc toc\"}")
                                             (defn send! [data]
                                               (fx/on-fx-thread
                                                (println "sending " data)
                                                (.executeScript engine (str "fromJava(\"" data "\")"))))
                                             (println "web view loaded."))}}}}))

(comment
 (send! "hello"))

