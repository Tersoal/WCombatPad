(ns WCombatPad.mat
  (:use hiccup.core)
  (:use hiccup.page-helpers)
  (:use hiccup.form-helpers)
  (:use [WCombatPad.data :only (get-combat-data
                                set-image-uri
                                get-state-list
                                change-grid
                                set-new-character
                                move-character
                                resize-character
                                kill-character
                                )])
  (:use [WCombatPad.images :only (save-image-file)])
  (:use [ring.util.response :only (redirect)])
  (:require (clojure.contrib [duck-streams :as ds])
  ))

(defn- get-map-headers [{ mat-name :name grid-size :grid-size [offset-x offset-y] :offset } script]
  (vec (concat
        [:head
   (include-css "/files/css/mat.css")]
   (if script
     [[:script {:type "text/javascript"} (str "gridSize=" grid-size"; offsetX=" offset-x "; offsetY=" offset-y"; combatName='" mat-name "';" )]
          (include-js 
            "/files/js/jquery-1.6.1.min.js" 
            "/files/js/jquery-ui-1.8.12.custom.min.js"
            "/files/js/mat.js")]))))


(defn- show-character-position [ grid-size [offset-x offset-y]  number {image :avatar  [x y] :pos char-name :name size :size}]
  [:img.token { :title char-name :id char-name :src (str "/remote/images/chars/" image) :style (str "z-index: 10; width:"
                                      (* size grid-size) "px; height:" (* size grid-size) "px; top:"
                                      (+ offset-y (* y grid-size)) "px; left:"
                                      (+ offset-x (- (* x grid-size) (* grid-size number))) "px;")} ])

(defn sum-chars-seq [characters]
  (reductions  #(+ %1 (%2 :size)) 0 characters))
(defn sum-chars [characters]
  (reduce #(+ %1 (%2 :size)) 0 characters))

(defn- show-mat [{combat-name :name mat :mat grid-size :grid-size offset :offset  characters :characters order :order}]
  (let [alive-characters (filter #(not (= (% :dead) "yes")) characters)]
  [:section#mat
   (map (partial show-character-position grid-size offset) (sum-chars-seq characters) alive-characters)
  [:div#position {:style (str "width:" (- grid-size 4) "px;"
                               "height:" (- grid-size 4) "px;"
                               ) } ""]
   [:img#map {:src (str "/combat/" combat-name "/map/" order) :style (str "left:-" (* grid-size (sum-chars alive-characters)) "px;") }]
   ]))


  
(defn- copy-avatar-form [pad-name copy-name image]
  [:section.copy_form
   "Copiar Avatar"
   (form-to [:post (str "/combat/" pad-name "/character")]
            (label "charname" "Nombre")
            (text-field "charname" copy-name)
            (submit-button "Copiar")
            (hidden-field "avatar" image) 
            (hidden-field "copy" "yes"))])

(defn- resize-avatar-form [pad-name char-name size]
  [:section.resize_form
   "Cambiar Tamaño"
   (form-to [:post (str "/combat/" pad-name "/resize")]
            (label "size" "Tamaño")
            (text-field "size" size)
            (hidden-field "name" char-name)
            (submit-button "Cambiar"))])


(defn- kill-avatar-form [pad-name char-name dead]
  [:section.kill_form
   (if (= dead "yes") "Revivir personaje" "Matar personaje")
   (form-to [:post (str "/combat/" pad-name "/kill")]
            (hidden-field "name" char-name)
            (hidden-field "dead" (if (= dead "yes") "no" "yes")) 
            (submit-button
             (if (= dead "yes") "Revivir" "Matar")))])


(defn generate-copy-name [char-name characters]
  (let [char-names (set (map :name characters))]
    (first (filter #(not (char-names %)) (map #(str char-name %)(iterate inc 1))))))
(defn accordion-header [data]
  [:a {:href "#" } [:div data]])

(defn- show-character [{char-name :name image :avatar size :size dead :dead } pad-name characters]
  [(accordion-header [:div.character-name
   [:img {:src (str "/remote/images/chars/" image) :width "30px" :height "30px" }]
   char-name (if (= dead "yes") " (Muerto)" "")])
   [:div.character
    (unordered-list [(copy-avatar-form pad-name (generate-copy-name char-name characters) image)
                     (resize-avatar-form pad-name char-name size)
                     (kill-avatar-form pad-name char-name dead)])
   ]])

(defn- show-characters [{characters :characters pad-name :name }]
  (vec (concat [:section#characters {:class "accordion"}]
   (reduce concat [] (map #(show-character % pad-name characters) characters)))))
(defn- multipart-form [form]
  (assoc form 1 (assoc (get form 1) :enctype "multipart/form-data" )))

(defn- upload-form [{pad-name :name}]
  [(accordion-header "Nueva imagen")
  [:section#upload_form
  (multipart-form (form-to [:post (str "/combat/" pad-name "/map")]
           (file-upload "image")
           (submit-button "Subir")))]])

(defn- change-grid-form [{pad-name :name
                          [posx posy] :offset
                          grid-size :grid-size}]
  [(accordion-header "Modificar rejilla")
  [:section#change_grid
   (form-to [:post (str "/combat/" pad-name "/grid")]
            (label "posx" "Offset X")
            (text-field "posx" posx)[:br]
            (label "posy" "Offset Y")
            (text-field "posy" posy)[:br]
            (label "gridsize" "Anchura")
            (text-field "gridsize" grid-size)[:br]
            (submit-button "Modificar"))]])

(defn- create-character [{pad-name :name}]
  [(accordion-header "Crear personaje")
  [:section#create_character
   (multipart-form (form-to
                    [:post (str "/combat/" pad-name "/character")]
                    (label "charname" "Nombre")
                    (text-field "charname")[:br]
                    (label "avatar" "Avatar")
                    (file-upload "avatar")[:br]
                    (submit-button "Crear")
                    ))]])


(defn- show-actions [combat-data] (vec (concat [:section#actions {:class "accordion"}]
                                     (upload-form combat-data)
                                     (change-grid-form combat-data)
                                     (create-character combat-data))))

(defn show-state [combat-name {order :order description :description}]
  [:div
   [:a { :href (str "/combat/" combat-name "/state/" order)} description]
   " "
   [:a { :href (str "/combat/" combat-name "/state/" order ".png")} " IMG"]])

(defn show-state-list [combat-name]
  [:section#states (unordered-list
                    (map #(show-state combat-name %)
                         (get-state-list combat-name)))])

(defn show-body [{ combat-name :name :as combat-data }]
  [:body (show-mat combat-data) 
           [:nav
            (show-actions combat-data)
            (show-characters combat-data) 
            (show-state-list combat-name)]])

(defn
  show-combat
  ([combat-name]
     (let [combat-data (get-combat-data combat-name)]
     (html5 (get-map-headers combat-data true)
           (show-body combat-data)
           )))
  ([combat-name order]
     (let [combat-data (get-combat-data combat-name order)]
       (html5
        (get-map-headers combat-data false)
        (show-mat combat-data)
        [:nav
         (show-state-list combat-name)
         [:a {:href (str "/combat/" combat-name) } "Volver"]]))))
  

(defn save-file [file-name dir stream]
  (save-image-file file-name dir stream)
 ; (ds/copy stream (ds/file-str (str "resources/public/images/" dir "/" file-name)))
  )

(defn save-image [combat-name {img-name :filename stream :tempfile}]
  (let [file-name (str combat-name img-name)]
  (do
    (save-file file-name "maps" stream)  
    (set-image-uri combat-name file-name)
    (redirect (str "/combat/" combat-name))
    )))

(defn save-character-file [ file-name { stream :tempfile}]
  (save-file file-name "chars" stream))

(defn save-character [combat-name character-name image copy]
  (let [file-name (if copy image (str combat-name character-name (:filename image)))]
    (do
      (if (not copy)
      (save-character-file file-name image))
      (set-new-character combat-name character-name file-name)
      (redirect (str "/combat/" combat-name)))))
      


(defn save-grid [combat-name posx posy grid-size]
  (do (change-grid combat-name posx posy grid-size)
      (redirect (str "/combat/" combat-name))))

(defn save-move [combat-name char-name posx posy]
  (do (move-character combat-name char-name [posx posy])
      (html (show-body (get-combat-data combat-name)))))

(defn save-resize [combat-name char-name size]
  (do (resize-character combat-name char-name size)
      (redirect (str "/combat/" combat-name))))
(defn save-kill [combat-name char-name dead]
  (do (kill-character combat-name char-name dead)
      (redirect (str "/combat/" combat-name))))