(ns shadow-cljfx.web-view
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

(defn wrap-instance
  "takes a lifecycle instance (which will not be modified) and a function that will
   be called on the Component's instance during the execution of the create method.
   It can be used to capture a reference towards the underlying component of a lifecycle."
  [lifecycle f]
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

;; based on cljfx.ext.web-view
;; I've removed props that I don't need and add/modify some others

(def engine-ext

  (fx/make-ext-with-props

   {:html     (prop/make
               (mutator/setter
                #(when %2 (.loadContent (.getEngine ^WebView %1) %2 "text/html")))
               lc/scalar)

    :url      (prop/make
               (mutator/setter
                #(when %2 (.load (.getEngine ^WebView %1) %2)))
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
                                    (changed [^ObservableValue _ov
                                              ^Worker$State _old-state
                                              ^Worker$State new-state]
                                      (if (= new-state Worker$State/SUCCEEDED)
                                        (f engine (.getDocument engine)))))))))
               lc/scalar)}))

(defprotocol ISend
  (send [_ data]))

(defn bridge [handler]
  (reify ISend
    (send [_ data] (handler (read-string data)))))

(defn web-view
  [{:keys [id url html on-error on-load handler]
    :or   {on-error (fn [e] (println "error: " e))
           on-load  (fn [_ _] nil)
           handler  (fn [message] (println "received: " message))}}]
  {:fx/type (wrap-instance engine-ext
                           (fn [instance]
                             (swap! engines assoc id (.getEngine ^WebView instance))))
   :desc    {:fx/type :web-view}
   :props   {:html     html
             :url      url
             :bridge   (bridge handler)
             :on-error on-error
             :on-load  on-load}})

(defn send! [id data]
  (fx/on-fx-thread
   (.executeScript (get @engines id)
                   (str "webView.send(" (pr-str data) ")"))))


