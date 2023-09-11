#!/usr/bin/env bb

(ns roundtrip
  (:require
   [clojure.string :as str]
   [clojure.pprint :as pp]
   [clojure.java.io :as io]
   [clojure.data.csv :as csv]
   [dbdoc :as dbd]))

(defn recase [s] (str/replace s #"_" "-"))

(defn parse-rows
  "Build up a sorted map in pretty org format for printing."
  [rows]
  (reduce (fn [acc [tname field desc]]
            (let [tname (recase tname) field (recase field)]
              (update acc tname conj (str "- " field " ::\n  " desc "\n"))))
          (sorted-map)
          rows))

(defn parse-tsv []
  (with-open [r (io/reader "dbdoc-public.tsv")]
    (let [rows (csv/read-csv r :separator \tab)]
      (prn (take 8 rows))
      (parse-rows rows))))

(defn main []
  (mapv (fn [[table items]]
          (println "*" table "\n")
          (mapv println items))
        (parse-tsv)))

(def xs
  [["api_requests_log" "data" "EDN with request data"]
   ["audit_trail_events" "change_type" "UPDATE, CREATE, etc"]
   ["audit_trail_events" "commit_user" "sfdc user ID"]
   ["audit_trail_events" "entity_name" "dsProject, dsAsset, etc"]
   ["audit_trail_events" "json_data" "JSON containing more SF details of event"]
   ["audit_trail_events" "record_id" "??"]
   ["auth_password_reset" "complete" "boolean indicating if reset finished"]
   ["auth_password_reset" "email" "user being reset"]])
;; (pp/pprint (parse-rows xs))
