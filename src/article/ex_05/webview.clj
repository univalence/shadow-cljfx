(ns article.ex-05.webview
  (:require
   [cljfx.api :as fx]
   [cljfx.prop :as prop]
   [cljfx.mutator :as mutator]
   [cljfx.lifecycle :as lifecycle]
   [cljfx.coerce :as coerce])
  (:import (javafx.beans.value ChangeListener ObservableValue)
           (javafx.concurrent Worker$State)
           (javafx.scene.web WebView)))

(def engines (atom {}))

;; based on cljfx.ext.web-view
;; I've removed props that I don't need and add/modify some others

(def engine-ext

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

;; IWebBridge instances are intended to bridge between web views and cljfx

(defprotocol IWebBridge
  (toJava [_ data]))

(def bridge_log
  (reify IWebBridge
    (toJava [_ data] (println "receive " data " from client."))))

(defn bridge [handler]
  (reify IWebBridge
    (toJava [_ data] (handler (read-string data)))))

(defn webview [{:keys [id markup on-error on-load handler]
                :or {bridge bridge_log
                     on-error (fn [e] (println "error: " e))}}]
  {:fx/type engine-ext
   :desc    {:fx/type :web-view}
   :props   {:html     markup
             :bridge   (if handler (bridge handler) bridge)
             :on-error on-error
             :on-load  (fn [engine document]
                         (swap! engines assoc id engine)
                         (when on-load (on-load engine document)))}})

(defn send! [id data]
  (fx/on-fx-thread
   (.executeScript (get @engines id)
                   (str "fromJava(\"" (pr-str data) "\")"))))