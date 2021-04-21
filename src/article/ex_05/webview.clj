(ns article.ex-05.webview
  (:refer-clojure :exclude [send])
  (:require
   [cljfx.api :as fx]
   [cljfx.prop :as prop]
   [cljfx.mutator :as mutator]
   [cljfx.lifecycle :as lc]
   [cljfx.component :as comp]
   [cljfx.coerce :as coerce])
  (:import (javafx.beans.value ChangeListener ObservableValue)
           (javafx.concurrent Worker$State)
           (javafx.scene.web WebView)))

(defonce engines (atom {}))

;; based on cljfx.ext.web-view
;; I've removed props that I don't need and add/modify some others

(defn wrap-instance [lifecycle f]
  (reify lc/Lifecycle
    (create [_ desc opts]
      (let [this (lc/create lifecycle desc opts)]
        (f (comp/instance this))
        this))
    ;; boilerplate
    (advance [_ component desc opts]
      (lc/advance lifecycle component desc opts))
    (delete [_ component opts]
      (lc/delete lifecycle component opts))))

(def engine-ext

  (fx/make-ext-with-props

   {:html     (prop/make
               (mutator/setter
                #(.loadContent (.getEngine ^WebView %1) %2 "text/html"))
               lc/scalar)

    :bridge   (prop/make
               (mutator/setter
                (fn [this bridge]
                  (let [engine (.getEngine ^WebView this)
                        window (.executeScript engine "window")]
                    (.setMember window "app" bridge))))
               lc/scalar)

    :on-error (prop/make
               (mutator/setter
                #(.setOnError (.getEngine ^WebView %1) %2))
               lc/event-handler
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
               lc/scalar)}))

;; IWebBridge instances are intended to bridge between web views and cljfx

(defprotocol ISend
  (send [_ data]))

(defn bridge [handler]
  (reify ISend
    (send [_ data] (handler (read-string data)))))

(def bridge_log
  (bridge (fn [message] (println "received: " message))))

(defn web-view [{:keys [id html on-error on-load handler]
                 :or   {on-error (fn [e] (println "error: " e))
                        on-load  (fn [_ _] nil)}}]
  {:fx/type (wrap-instance engine-ext
                           (fn [instance]
                             (swap! engines assoc id (.getEngine ^WebView instance))))
   :desc    {:fx/type :web-view}
   :props   {:html     html
             :bridge   (if handler (bridge handler) bridge_log)
             :on-error on-error
             :on-load  on-load}})

(defn send! [id data]
  (fx/on-fx-thread
   (.executeScript (get @engines id)
                   (str "webView.send(" (pr-str data) ")"))))

(comment
 (get @engines :ex-05))
#_(fx/on-fx-thread (.executeScript (get @engines :ex-05) "window"))