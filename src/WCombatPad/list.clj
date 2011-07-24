(ns WCombatPad.list
  (:use hiccup.core)
  (:use hiccup.page-helpers)
  (:use hiccup.form-helpers)
  (:use [ring.util.response :only (redirect)])
  (:use [ WCombatPad.data :only (get-pad-list create-pad)]))


(defn new-combat [combat-name] (do (create-pad combat-name)(redirect "/")))
(defn- link-to-combat [{id :_id, combat-name :name}] [:a {:href (str "/combat/" id)} combat-name])
(defn show-list "Shows the list of the combats" [] 
  (html5 (unordered-list
          (conj
           (map link-to-combat (get-pad-list))
           (form-to [:post "/combat"]
                    (label "matname" "nuevo tablero")
                    (text-field "matname" "")
                    (submit-button "Crear"))
           ))))
   