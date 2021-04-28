# Clojurescript webViews

Nous developpons en ce moment chez Univalence un client lourd basé sur JavaFX. Pour ce, nous utilisons comme langage principal clojure et l'excellente librarie cljfx.

En cours de route, nous avons vite réalisé l'attrait que constituent les WebViews de JavaFX, nous donnant accès à tout l'écosystème web.

Dans cet article nous allons proposer une manière de brancher des webviews écrites en clojurescript sur une application cljfx. L'objectif étant de profiter du merveilleux workflow de dev apporté par clojurescript + shadow-cljs au sein d'une application cljfx.

## cljfx

Tout d'abord voyons à quoi peut ressembler une application cljfx minimale.

`deps.edn`
```clojure
{:paths   ["src"],
 :deps
          {org.clojure/clojure           {:mvn/version "1.10.0"},
           cljfx/cljfx                   {:mvn/version "1.7.13"}},

```

`src/article/ex_01.clj`
```clojure
(ns article.ex-01
  (:require [cljfx.api :as fx]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :width 80
   :height 80
   :scene {:fx/type :scene
           :root {:fx/type :label
                  :text " Bonjour !"}}}))

```

![[shadow-cljfx/blob/master/doc/images/Capture d’écran 2021-04-13 à 10.50.00.png]]
Pour plus de détails, je vous invite à regarder directement le [repo github de cljfx](https://github.com/cljfx/cljfx). Ce dernier regorge d'exemples permettant de rapidement ce faire une idée de ce qu'il est possible d'en tirer, et ce même sans expérience antérieure avec javaFX.

Pour l'heure nous allons essayer d'afficher une simple vue web:

`src/article/ex_02.clj`
```clojure
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
```

![[shadow-cljfx/blob/master/doc/images/Capture d’écran 2021-04-13 à 10.49.23.png]]

## clojurescript

Nous allons maintenant tenter d'utiliser clojurescript pour définir une vue web.

Tout d'abord, créons un namespace clojurescript contenant un composant minimal:

`src/article/ex_03/vue.cljs`
```clojure
(ns article.ex-03.vue  
 (:require [reagent.dom :as rd]))  
  
(def markup  
  [:h1 "Hello shadow-cljx"])  
  
(defn ^:export init []  
 (rd/render markup  
            (.getElementById js/document "app")))
```

Ensuite, utilisons shadow-cljs pour compiler notre composant et enveloppons le javascript en resultant dans une page html avec l'aide de hiccup. Enfin, affichons la page en question à l'aide d'une vue web.

`src/article/ex_03.clj`
```clojure
(ns article.ex-03
  (:require
   [cljfx.api :as fx]
   [cljfx.ext.web-view :as fx.ext.web-view]
   [hiccup.page :refer [html5]]
   [shadow.cljs.devtools.api :as shadow]))

(def OUTPUT_DIR "target/ex-03")

(def build-conf
  {:build-id   :ex-03
   :output-dir OUTPUT_DIR
   :target     :browser
   :modules    {:main {:init-fn 'article.ex-03.vue/init}}})

(defonce COMPILED_JS
  (shadow/with-runtime
   (shadow/release* build-conf {})
   (slurp (str OUTPUT_DIR "/main.js"))))

(def MARKUP
  (html5
   [:head
    [:meta {:charset "utf-8"}]]
   [:body
    [:div#app]
    [:script COMPILED_JS]]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type fx.ext.web-view/with-engine-props
                       :desc    {:fx/type :web-view}
                       :props   {:content MARKUP}}}}))
```

![[shadow-cljfx/blob/master/doc/images/Capture d’écran 2021-04-20 à 12.44.15.png]]

## Hot reloading ?

Une des motivations premières de cette expérience était d'avoir accès aux commodités du development interactif qu'offrent clojurescript + shadow-cljs.

### setup minimal shadow-cljs

`shadow-cljs.edn`
```clojure
{:source-paths ["src"]
 :dev-http {8080 "target/"}
 :builds {:app {:output-dir "target/"
                :target :browser
                :modules {:main {:init-fn app.main/main!}}}}}
```

`src/app/main.cljs`
```clojure
(ns app.main)

(defn ^:dev/after-load reload []
  (println "reloaded"))
  
(defn ^:export init []
  (println "init"))
```

`target/index.html`
```html
<!DOCTYPE html>  
<html>
<head><meta charset="utf-8"></head>
<body>
  <div>see console!</div>
  <script src="main.js"></script>
</body>
</html>
```

Avec ces trois fichiers vous pouvez executer la commande suivante depuis la racine du projet:

`shadow-cljs watch app`

Puis visiter `http://localhost:8080`

Si maintenant vous éditez `main.cljs` il y a recompilation et rafraichissement hot reloading de votre code.

### Integration cljfx

Pour embarquer ce mécanisme au sein d'une application cljfx, c'est simple, il suffit de passer cette url (`http://localhost:8080`) à une web view comme ça:

```clojure
(ns article.ex-04
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as fx.ext.web-view]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type fx.ext.web-view/with-engine-props
                  :desc    {:fx/type :web-view}
                  :props   {:url "http://localhost:8080"}}}}))
```

Cet exemple est quasiment identique à l'exemple 2, sauf que l'on passe une url au lieu d'une string html.

Si vous rééditez le code de main.cljs la mécanique de recompilation/hot-reloading est bien propagée à l'interieur de notre webView.

## Communication

Nous allons maintenant tenter de mettre en place un moyen de communication entre l'application et notre webview. De chaque coté nous devrons définir un handler que le coté opposé pourra appeler.

Des informations nécéssaires à ce branchement se trouvent dans la [documentation du WebEngine](https://openjfx.io/javadoc/14/javafx.web/javafx/scene/web/WebEngine.html). Pour avoir accès au WebEngine de notre WebView nous utiliserons le [mecanisme d'extension de cljfx](https://github.com/cljfx/cljfx#extending-cljfx).

Pour pouvoir envoyer des messages de la WebView vers notre application nous allons attacher un object global au DOM de notre WebView. La documentation du WebEngine donne cet exemple:

```java
public class JavaApplication {
    public void exit() {
        Platform.exit();
    }
}
...
JavaApplication javaApp = new JavaApplication();
JSObject window = (JSObject) webEngine.executeScript("window");
window.setMember("app", javaApp);
```


Dans le cas présent, l'object que nous passons à `window.setMember` devra posseder une methode `send` que le client pourra utiliser pour communiquer avec l'application. En clojure on peut faire ça comme ça:

```clojure
(defprotocol ISend 
  (send [this message]))
  
(defn sender 
  "turn the given function into an instance of ISend"
  [f] 
  (reify ISend 
    (send [_ message] (f message))))
```

Depuis clfx nous pourrons avoir accès au WebEngine via une extension cljfx, en nous basant sur l'[extension de base](https://github.com/cljfx/cljfx/blob/master/src/cljfx/ext/web_view.clj) de la librarie.

```clojure
(ns article.ex-05
  (:require
   [cljfx.api :as fx]
   [cljfx.prop :as prop]
   [cljfx.mutator :as mutator]
   [cljfx.lifecycle :as lifecycle])
  (:import (javafx.scene.web WebView)))

(def engine-ext

  (fx/make-ext-with-props

   {:html     (prop/make
               (mutator/setter
                #(.loadContent (.getEngine ^WebView %1) %2 "text/html"))
               lifecycle/scalar)

    :handler  (prop/make
               (mutator/setter
                (fn [this f]
                  (let [engine (.getEngine ^WebView this)
                        window (.executeScript engine "window")]
                    (.setMember window "app" (sender f)))))
               lifecycle/scalar)}))
```

Cette extension pourra être utilisée ainsi:

```clojure
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
                       :props   {:html     html-content
                                 :handler  (fn [data] (println "Client sent: " data))}}}}))
```

![[shadow-cljfx/blob/master/doc/images/Capture d’écran 2021-04-21 à 13.24.27.png]]

Lorsque l'on clique sur le bouton "anyone here ?" le message `Client sent: anyone here ?` est affiché coté application.

Pour que l'application puisse envoyer des message à la WebView il va nous falloir définir un handler coté WebView et être en mesure de l'appeler coté application. 

Pour la definition nous modifions simplement notre `html-content` en lui ajoutant un script declarant une variable `webView` contenant un objet disposant d'une méthode `send`. Dans cet exemple cette méthode remplacera le contenu de la div `#app` par le message qu'elle reçoie.

```clojure
(def html-content
  "<!DOCTYPE html><html>
    <body>
      <div id=\"app\"> Hello </h1>
      <button onclick=\"app.send('anyone here ?')\">anyone here ?</button>
	  
	  // new -----------------------------------------------
      <script>
        var webView = {send: function(message){
          document.getElementById('app').innerHTML = message;
          }
        }
      </script>
	  // ---------------------------------------------------
	  
    </body>
   </html>")
```

Pour évaluer du code javascript dans le contexte de notre WebView depuis l'application nous utiliserons la méthode `executeScript` du WebEngine de notre WebView. Il va donc nous falloir capturer une référence vers ce WebEngine.

Dans clfx chaque composant est représenté à l'aide d'une abstraction portant le nom de `Lifecycle` et dont voici la définition:

```clojure
(defprotocol Lifecycle
  :extend-via-metadata true
  (create [this desc opts])
  (advance [this component desc opts])
  (delete [this component opts]))
```

[documentation ici](https://github.com/cljfx/cljfx#how-does-it-actually-work)

Il serait donc possible d'envelopper le lifecycle representant notre webView de manière à capturer le WebEngine lorsque la methode `create` est appelée.

Pour ça on peut introduire la fonction `wrap-instance`:

```clojure
(defn wrap-instance [lifecycle f]
  (reify lc/Lifecycle
    
	;; interesting part
    (create [_ desc opts]
      (let [this (lc/create lifecycle desc opts)]
        (f (comp/instance this)) ;; <-- calling f on the created instance
        this))
		
    ;; boilerplate
    (advance [_ component desc opts]
      (lc/advance lifecycle component desc opts))
    (delete [_ component opts]
      (lc/delete lifecycle component opts))))
```

Cette fonction pourra être appelé avec notre extension (qui est vous l'aurez compris une instance de Lifecycle)

```
;; a fresh atom to hold our engine reference
(def engine (atom nil))

;; wrapping our extension to capture the engine
(wrap-instance engine-ext 
  #(reset! engine (.getEngine %)))

```

À la suite de quoi nous pouvons définir une méthode `send!` qui utilisera cette reference pour envoyer un message à la webView.

Voici le code complet:

```clojure
(ns article.ex-05
  (:require
   [cljfx.api :as fx]
   [cljfx.prop :as prop]
   [cljfx.mutator :as mutator]
   [cljfx.component :as comp]
   [cljfx.lifecycle :as lc])
  (:import (javafx.scene.web WebView)))

(defprotocol ISend
  (send [this message]))

(defn sender
  "turn the given function into an instance of ISend"
  [f]
  (reify ISend
    (send [_ message] (f message))))

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

   {:html    (prop/make
              (mutator/setter
               #(.loadContent (.getEngine ^WebView %1) %2 "text/html"))
              lc/scalar)

    :handler (prop/make
              (mutator/setter
               (fn [this f]
                 (let [engine (.getEngine ^WebView this)
                       window (.executeScript engine "window")]
                   (.setMember window "app" (sender f)))))
              lc/scalar)}))

(def html-content
  "<!DOCTYPE html><html>
    <body>
      <div id=\"app\"> Hello </h1>
      <button onclick=\"app.send('anyone here ?')\">anyone here ?</button>
      <script>
        var webView = {send: function(message){
          document.getElementById('app').innerHTML = message;
          }
        }
      </script>
    </body>
   </html>")

(def engine (atom nil))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type (wrap-instance engine-ext #(reset! engine (.getEngine %)))
                       :desc    {:fx/type :web-view}
                       :props   {:html        html-content
                                 :handler     (fn [data] (println "Client sent: " data))}}}}))

(defn send! [message]
  (fx/on-fx-thread
   (.executeScript @engine (str "webView.send(" (pr-str message) ")"))))


(comment
 (send! "Bonjour"))
```


## Avec clojurescript

Dans l'exemple précédent nous avons écarté clojurescript de l'équation pour ne pas compliquer les choses outre mesure. Nous allons le rebrancher maintenant.

Nous en profiterons pour extraire l'implementation de la communication application/webView vers un namespace dédié exposant un moyen de créer des webView et de communiquer avec elles.

```clojure
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
  "takes a lifecycle instance which will not be modified.
   and a function that will be called on the Component's instance during the execution of the create method.
   It can be used to capture a reference towards the underlying component of a lifecycle"
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
                                    (changed [^ObservableValue ov
                                              ^Worker$State old-state
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

```

Ce namespace pourra être utilisé comme une library exposant deux fonctions:
* web-view
* send!

Nous allons donc maintenant l'utiliser sur notre exemple précédent (celui utilisant clojurescript).

`shadow_cljfx/example/view.cljs`

```clojure
(ns shadow-cljfx.example.view
  (:require [reagent.dom :as rd]
            [reagent.core :as r]))

(def state (r/atom {:message "Hello shadow-cljx"}))

(defn root []
  [:div#app
   [:h1 (:message @state)]
   [:button {:on-click (fn [_] (js/window.app.send "toc toc"))}
    "knockin on server's door"]])

(defn ^:export init []

  (set! js/window.webView #js {})

  (set! js/window.webView.send
        (fn [data] (swap! state assoc :message data)))

  (rd/render [root]
             (.getElementById js/document "app")))
```

`shadow_cljfx/example/core.clj`

```clojure
(ns shadow-cljfx.example.core
  (:require
   [cljfx.api :as fx]
   [shadow-cljfx.web-view :as wv]
   [hiccup.page :refer [html5]]
   [shadow.cljs.devtools.api :as shadow]))

(def OUTPUT_DIR "target/shadow_cljfx/example")
(def ID :shadow-cljfx.example)

(def build-conf
  {:build-id   ID
   :output-dir OUTPUT_DIR
   :target     :browser
   :modules    {:main {:init-fn 'shadow-cljfx.example.vue/init}}})

(def COMPILED_JS
  (shadow/with-runtime
   (shadow/release* build-conf {})
   (slurp (str OUTPUT_DIR "/main.js"))))

(def MARKUP
  (html5
   [:head
    [:meta {:charset "utf-8"}]]
   [:body
    [:div#app]
    [:script COMPILED_JS]]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :x       1000 :y -1000
   :scene   {:fx/type :scene
             :root    {:fx/type wv/web-view
                       :id      ID
                       :html    MARKUP
                       :handler (fn [message] (println "received: " message))
                       :on-load (fn [_ _] (println "web view loaded."))}}}))

(comment
 (wv/send! ID "hello")
 (println MARKUP))

```

Et voilà !

## Quelques commodités supplémentaires

Dans notre précédent exemple, nous avons fait fi du hot-reloading et laissé à la charge du developpeur pas mal de boilerplate.

shadow-cljfx expose le namespace `shadow-cljfx.repl` pour vous aidez avec tout ça.

Ce namespace vous permet d'initialiser de nouvelles webViews facilement. Dans votre projet, depuis un repl vous pouvez essayer ceci:

```clojure

(require '[shadow-cljfx.repl :as repl])

(repl/new-web-view :foo.bar)

```

Cela créera pour vous les fichiers nécessaires à l'utilisation et au developpement d'une nouvelle webView. Vous pouvez maintenant ouvrir le fichier `src/shadow_cljfx/example2/core.clj` fraichement créé et charger ce namespace dans votre REPL. à la fin du fichier vous trouverez quelque commandes interessantes.

```clojure 
(comment 
 (require '[shadow-cljfx.repl :as repl])  
 (repl/dev! ID)  
 (e> (js/console.log "hey"))  
 (wv/send! ID "hello")  
 (repl/compile-inline-index ID))
```


`(repl/dev! ID)` lance le mode dev pour votre webView. Concretement cela demarre un server shadow-cljs, un watcher et tout le tintouin. Après cela vous serez en mesure d'évaluer du code clojurescript dans le contexte de votre webView via la macro `e>` comme le fait justement la ligne suivante: `(e> (js/console.log "hey"))`.

Ce moyen de communication avec la webView est reservé à la phase de developpement et repose sur shadow-cljs.

Après cela, nous avons un exemple d'envoi de message via la fonction `send!`
dont nous avons parlé precédement. Si vous executez cette forme, le message d'accueil sera remplacé par "hello". Pour comprendre pourquoi, vous pouvez maintenant jeter un oeil au fichier `src/shadow_cljfx/example2/view.cljs`, cela devrait vous rappeler le code des exemples discutés plus haut.

Enfin, la forme `(repl/compile-inline-index ID))` vous permet de compiler le html definitif de cette webView vers `target/shadowcljfx/example2/cljfx.html` . A la suite de quoi notre webView pourra être utiliser sans shadow-cljs. 

Le namespace `shadow-cljfx.repl` expose également d'autre fonctionalités dont voici un aperçu:

```clojure
(watch id) ;; wrap shadow-cljs.devtools.api/watch
(watch-all!) ;; watch all cljfx registered webViews

(release id) ;; wrap shadow-cljs.devtools.api/release
(release-all!) ;; release all cljfx registered webViews

(render-comp id) ;; render the corresponding webView via cljfx

(delete-view id) ;; remove all folders and config related to the given id
```

