(ns shadow-cljfx.repl
  (:require [me.raynes.fs :as fs]
            [clojure.string :as str]
            [zprint.core :as zp :refer [zprint czprint]]
            [hiccup.page :refer [html5]]
            [backtick :refer [template]]
            [cljfx.api :as fx]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as shadow-server]
            [clojure.java.shell :refer [sh]]
            [shadow-cljfx.web-view :as wv]))

(def DEFAULT_SHADOW_CONFIG
  {:deps     true,
   :nrepl    {:port 8777},
   :dev-http {8080 "target/"},
   :builds   {}})

(def INDEX_PAGE
  (html5
   [:head
    [:meta {:charset "utf-8"}]]
   [:body
    [:div#app]
    [:script {:src "main.js"}]]))

(do :help

    (defn pretty-string [x]
      (with-out-str (zprint x)))

    (defn pretty-spit [path data]
      (spit path
            (pretty-string data)))

    (defn pretty-file-str [exprs]
      (apply str (interpose "\n\n" (map pretty-string exprs))))

    (defn dashes->underscores [s]
      (str/replace s #"-" "_"))

    (defn build-id->path [build-id]
      (-> (name build-id)
          (str/replace #"\." "/")
          (dashes->underscores)))

    (defn build-id->js-entry-point [build-id]
      (symbol (str (name build-id) ".view/init"))))

(defn shadow-build-conf [build-id]
  {:cljfx       true
   :output-dir  (str "target/" (build-id->path build-id))
   :asset-path  "."
   :target      :browser
   :modules     {:main {:init-fn (build-id->js-entry-point build-id)}}
   :build-hooks [(list `release-hook)]})

(defn ensure-shadow-config []
  (or (fs/exists? "shadow-cljs.edn")
      (pretty-spit "shadow-cljs.edn"
                   DEFAULT_SHADOW_CONFIG)))

(defn get-shadow-conf []
  (ensure-shadow-config)
  (read-string (slurp "shadow-cljs.edn")))

(defn get-cljfx-build-ids []
  (let [conf (get-shadow-conf)]
    (keep (fn [[id {:keys [cljfx]}]] (when cljfx id)) (:builds conf))))

(defn existing-build? [id]
  (contains? (set (get-cljfx-build-ids)) id))

(defn emit-view-cljs [build-id]
  (pretty-file-str
   (template
    [(ns ~(symbol (str (name build-id) ".view"))
       (:require [reagent.dom :as rd]
        [reagent.core :as r]))

     (def state (r/atom {:message "Hello shadow-cljx"}))

     (defn root []
       [:div
        [:h1 (:message @state)]
        [:button {:on-click (fn [_] (js/window.app.send "toc toc"))}
         "knockin on server's door"]])

     (defn render {:dev/after-load true} []
       (rd/render [root] (js/document.getElementById "app")))

     (defn init {:export true} []
       (set! js/window.webView (js-obj "send" (fn [data] (swap! state assoc :message data))))
       #_(set! js/window.webView.send
             (fn [data] (swap! state assoc :message data)))
       (render))])))

(defn emit-core-clj [build-id]
  (let [base-path (build-id->path build-id)]
    (pretty-file-str
     (template
      [(ns ~(symbol (str (name build-id) ".core"))
         (:require
          [cljfx.api :as fx]
          [shadow-cljfx.web-view :as wv]))

       (def ID ~build-id)

       (defn component [{:as opts}]
         (merge {:fx/type wv/web-view
                 :id      ~build-id
                 :handler (fn [message] (println "received: " message))
                 :on-load (fn [_ _] (println "web view loaded."))}
                (if (:dev opts)
                  {:url ~(str "http://localhost:8080/" base-path)}
                  {:html (slurp ~(str "target/" base-path "/cljfx.html"))})
                opts))

       (defmacro e> [& code]
         (list (list `requiring-resolve 'shadow.cljs.devtools.api/cljs-eval)
               ID
               (str (cons 'do code))
               {}))

       (comment

        (require '[shadow-cljfx.repl :as repl])
        (repl/dev! ID)
        (e> (js/console.log "hey"))
        (wv/send! ID "hello")
        (repl/compile-inline-index ID))]))))

(defn new-web-view [build-id]
  (ensure-shadow-config)
  (when (existing-build? build-id)
    (throw (Exception. (str "this web view has already been created: " build-id
                            "\nplease consider using (delete-web-view " build-id ")"))))
  (let [path-suffix (build-id->path build-id)
        src-path (str "src/" path-suffix)
        target-path (str "target/" path-suffix)
        shadow-conf (assoc-in (read-string (slurp "shadow-cljs.edn"))
                              [:builds build-id]
                              (shadow-build-conf build-id))]
    ;; sources
    (fs/mkdirs src-path)
    (spit (str src-path "/view.cljs")
          (emit-view-cljs build-id))
    (spit (str src-path "/core.clj")
          (emit-core-clj build-id))

    ;; target
    (fs/mkdirs target-path)
    (spit (str target-path "/index.html")
          INDEX_PAGE)

    ;; shadow-conf
    (pretty-spit "shadow-cljs.edn"
                 shadow-conf)))

(defn release-build? [str]
  (str/starts-with? str "var shadow$provide = {};\n(function(){"))

(defn spit-cljfx-html [base-path js]
  (spit (str base-path "/cljfx.html")
        (html5
         [:head [:meta {:charset "utf-8"}]]
         [:body [:div#app] [:script js]])))

(defn compile-cljfx-html
  [build-id]
  (let [base-path (str "target/" (build-id->path build-id))
        js (slurp (str base-path "/main.js"))]
    (if (release-build? js)
      (spit-cljfx-html base-path js)
      (shadow/release build-id {}))))

(defn release-hook
  {:shadow.build/stage :optimize-finish}
  [build-state build-id]
  (println "compiling cljfx.html")
  (let [base-path (str "target/" (build-id->path build-id))
        js (slurp (str base-path "/main.js"))]
    (spit-cljfx-html base-path js)
    build-state))



(defn release-all! []
  (doseq [id (get-cljfx-build-ids)]
    (compile-cljfx-html id)))

(defn watch-all! []
  (shadow-server/start!)
  (doseq [id (get-cljfx-build-ids)]
    (shadow/watch id)))

(defn delete-view [build-id]
  (when-not (existing-build? build-id)
    (throw (Exception. (str "unknown view: " build-id))))
  (let [path-suffix (build-id->path build-id)
        src-path (str "src/" path-suffix)
        target-path (str "target/" path-suffix)
        shadow-conf (update (read-string (slurp "shadow-cljs.edn"))
                            :builds dissoc build-id)]
    (fs/delete-dir src-path)
    (fs/delete-dir target-path)
    (pretty-spit "shadow-cljs.edn" shadow-conf)))

(defn render-comp [build-id & [opts]]
  (let [target-base-path (str "target/" (build-id->path build-id))
        web-view-opts
        (merge (if (:dev opts)
                 {:url (str "http://localhost:8080/" (build-id->path build-id))}
                 {:html (slurp (str target-base-path "/cljfx.html"))})
               (assoc opts :id build-id))]
    (fx/on-fx-thread
     (fx/create-component
      {:fx/type :stage
       :showing true
       :scene   {:fx/type :scene
                 :root    (wv/web-view web-view-opts)}}))))



(do :aliases
    (def start-server! shadow-server/start!)
    (def stop-server! shadow-server/stop!)
    (def release shadow/release)
    (def watch shadow/watch))

(defn dev! [build-id]
  (start-server!)
  (watch build-id)
  (render-comp build-id {:dev true}))

(comment

 (delete-view :shadow-cljfx.example2)
 (new-web-view :shadow-cljfx.example2)
 (dev! :shadow-cljfx.example2)

 (watch-all!)
 (shadow-server/stop!)
 (new-web-view 'foo.bar)
 (new-web-view 'foo.baz)

 (existing-build? :shadow-cljfx.example2 )
 (get-cljfx-build-ids)



 (new-web-view :shadow-cljfx.example2)
 (start-server!)
 (watch :shadow-cljfx.example2)
 (repl :shadow-cljfx.example2)
 (stop-server!)
 (shadow/release! :shadow-cljfx.example2)
 (render-comp :shadow-cljfx.example2 {:dev true})

 (defmacro ex2> [& code]
   `(shadow/cljs-eval :shadow-cljfx.example2 ~(str (cons 'do code)) {}))

 (ex2> (js/console.log "pouet")
       (println "youyou"))

 (shadow/nrepl-select :shadow-cljfx.example2)
 (shadow/repl :shadow-cljfx.example2)

 (require '[shadow-cljfx.web-view :as wv])
 (render-comp (wv/web-view {:url "http://localhost:8080/foo/bar"}))
 (compile-cljfx-html :foo.bar)
 (compile-all)
 (render-comp (wv/web-view {:html (slurp "target/foo/bar/cljfx.html")})))

(comment :nrepl-connect

         (require '[clojure.tools.nrepl :as repl])

         (with-open [conn (repl/connect :port 8777)]
           (-> (repl/client conn 1000) ; message receive timeout required
               (repl/message {:op "eval" :code (repl/code (shadow/cljs-eval :shadow-cljfx.example2 "(js/console.log \"io\" )" {}))})
               repl/response-values
               ))



         (shadow/cljs-eval :shadow-cljfx.example2 "(js/console.log \"io\" )" {})
         )