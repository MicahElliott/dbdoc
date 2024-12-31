#!/usr/bin/env bb

;; Not working flexibly when classpath specified
;#!/usr/bin/env bb --classpath ...proj/dbdoc --main dbdoc

;; Convert a simple ORG file into SQL COMMENTs for documenting tables
;; and their columns.
;;
;; Environmnent variables for controlling behavior:
;; - DBDOC_SQL
;; - DBDOC_ORG
;; - DBDOC_HTML

(ns dbdoc
  (:require
   [babashka.fs :as fs]
   [clojure.pprint :as pp]
   [clojure.data.csv :as csv]
   [clojure.java.shell :refer [sh]]
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (java.time.format DateTimeFormatter)
   (java.time LocalDateTime))) ; not really needed since babashka

;; https://stackoverflow.com/a/62970721/326516
(def timestamp
  "Migration-compatible timestamp (as seen in Migratus and elsewhere).
  eg, `20210205111716`"
  (.format (LocalDateTime/now) (DateTimeFormatter/ofPattern "yyyyMMddHHmmss")))

;; Default files for user to configure
(def sql-file "Output SQL (migration) file containing timestamp"
  (or (System/getenv "DBDOC_SQL")
      (format "resources/migrations/%s-dbdoc.up.sql" timestamp)))
(def mig-dir "Migration directory, for deletion of old migs"
  (.getParent (io/file sql-file)))
(def org-file "Input file containing table/col descriptions"
  (let [f (or (System/getenv "DBDOC_ORG")  "docs/dbdoc.org")]
    (if-not (fs/exists? f)
      (do (println "ERROR: input org-file does not exist")
          (System/exit 1))
      f)))
(def html-file "Output HTML file for publishing"
  (or (System/getenv "DBDOC_HTML") "docs/dbdoc.html"))

(def roundtrip? "Run in round-trip mode: only generate diff"
  (atom false))

;
;;; Translation

(defn check-dir
  "Check for existence of directory for file `f`."
  [f]
  ;; (println "Checking for existence of directory:" f)
  (let [dir (.getParent (io/file f))]
    (when-not (.isDirectory (io/file dir))
      (println "ERROR:" dir "directory does not exist.")
      (System/exit 1))))

(defn xlate-col
  "Construct a dotted `schema.table.column` name.
  Replace hyphens w/ underscores, add `public` if was implicit."
  [tab col]
  (let [colname (-> (str tab "." (str/replace col #"(?s)^- (.*) ::.*" "$1"))
                    (str/replace  #"-" "_"))
        fieldsv (str/split colname #"\.") ; customer.active OR myschema.customer.active
        fields  (str/join \tab (if (= 2 (count fieldsv)) (into ["public"] fieldsv) fieldsv))
        text    (-> (str/join " " (rest (str/split col #"\n")))
                    str/trim
                    (str/replace  #"(?s)\s+" " ")
                    (str/replace  #"(?s)'" "’"))]
    [colname fields text]))
;; (xlate-col "myschema.mytab" "- foo ::\n  This is the description.")

(defn filt-gt1 [fields] (seq (filter #(< 1 (second %)) (frequencies fields))))

(defn check-duplicate-fields [tabname fieldstrs]
  (let [fields (reduce (fn [acc cur] (cons (first (xlate-col tabname cur)) acc)) [] fieldstrs)]
    (when-let [gt1s (filt-gt1 fields)] (println "WARNING: duplicate fields:" gt1s))))

(defn process-col
  "Convert a column description into a SQL COMMENT.
  Write roundtrip TSV when in mode."
  [tab col]
  (let [[colname fields text] (xlate-col tab col)
        cmt           (format "COMMENT ON COLUMN %s IS '%s';\n--;;" colname text)]
    (when @roundtrip? (.println *err* (format "%s\t%s" fields text)))
    cmt))

(comment ; get fields for single section, for matching with roundtrip tsv
  (def org (slurp "docs/dbdoc.org"))
  (def sec (first (rest (str/split org #"(?m)^\* "))))
  (let [[tab _ & defs] (str/split sec #"\n\n")]
    [tab (mapv #(apply str (drop 2 (first (str/split % #" ::")))) defs)])
  ;;=> ["film" ("title" "description" "original-language-id" "rental-rate" "length" "rating")]
  :end)

(defn xlate-section
  "Convert a section of table descriptions and combine with columns."
  [sec]
  (let [[tabname tabdesc & cols] (str/split sec #"\n\n")
        tabname  (-> tabname
                     (str/replace #"(?s)\n" " ")
                     (str/replace #"-" "_"))
        tabdesc  (-> tabdesc
                     (str/replace #"(?s)\n" " ")
                     (str/replace #"(?s)'" "’")
                     str/trim)]
    [tabname cols tabdesc]))

(defn check-duplicate-sections [secstrs]
  ;; TODO Inside the reduce fn is the place to check for dupe fields, if that's important enough
  (let [secs (reduce (fn [acc cur]
                       (let [[tabname cols _] (xlate-section cur)]
                         (check-duplicate-fields tabname cols)
                         (cons tabname acc)))
                     [] secstrs)]
    (when-let [gt1s (filt-gt1 secs)] (println "WARNING: duplicate tables:" gt1s))))

;; (seq (filter #(< 1 (second %)) (frequencies ["film" "rental" "actor" "fooschema.customer" "film"])))
;; => {"film" 2, "rental" 1, "actor" 1, "fooschema.customer" 1}

(defn process-section
  "Create section COMMENTs."
  [sec]
  (let [[tabname cols tabdesc] (xlate-section sec)
        ;; _      (println (format "%s\t%s" tabname tabdesc))
        cmt      (format "COMMENT ON TABLE %s IS '%s';\n--;;" tabname tabdesc)]
    (cons cmt (mapv #(process-col tabname %) cols))))

(defn move-old-migration
  "Rename older migration file(s).
  Assume was successful in creating a new mig."
  []
  (when-let [old (first (fs/glob mig-dir "*-dbdoc.up.sql" {:max-depth 1}))]
    (if (= 0 (:exit (sh "git" "ls-files" "--error-unmatch" (.toString old))))
      (do (println "Moving old COMMENTs mig via git:    " (.toString old))
          (sh "git" "mv" (.toString old) sql-file))
      (println "WARNING: Detected old dbdoc file not in git:" (.toString old)))))

(defn print-comments-file ; FIXME change name
  "Write all SQL comments to `sql-file`."
  [title comments]
  (move-old-migration)
  (println "To new SQL COMMENTs file:           " sql-file)
  (binding [*out* (io/writer sql-file)]
    (println title)
    (println "-- DO NOT EDIT THIS FILE; SEE dbdoc.org FILE")
    ;; XXX No way to know the name of the database!
    ;; Unless we look for env var like PGDATABASE
    ;; (println (format "COMMENT ON DATABASE %s IS '%s';\n--;;" ??? dbdesc))
    ;; TODO Could wrap in a BEGIN/COMMIT transaction here, but some migrators
    ;;      do this automatically
    (doseq [c comments]
      (println)
      (doseq [i c]
        (println i)))))

;
;;; Roundtrip (none of these actually used; failed experiment)

(defn recase [s] (str/replace s #"_" "-"))

(defn rt-parse-rows
  "Build up a sorted map in pretty org format for printing."
  [rows]
  (reduce (fn [acc [tname field desc]]
            (let [tname (recase tname), field (recase field)]
             (update acc tname conj (str "- " field " ::\n  " desc "\n"))))
          (sorted-map)
          rows))

(defn rt-parse-tsv []
  (with-open [r (io/reader "dbdoc-public.tsv")]
    (let [rows (csv/read-csv r :separator \tab)]
      (prn (take 8 rows))
      (rt-parse-rows rows))))

(defn roundtrip []
  (println "Running round-trip. Paste the following into appropriate places in your dbdoc.org file.\n")
  (mapv (fn [[table items]]
          (println "*" table "\n")
          (mapv println items))
        (rt-parse-tsv)))

;
;;; Main

(defn genmig
  "Normal main, generate a migration from dbdoc.org file."
  []
  (check-dir sql-file)
  (let [;; To test, set DBDOC_ORG=example.org
        _        (println "Reading ORG descriptions input file:" org-file)
        org      (slurp org-file)
        title    (str/replace (first (str/split org #"\n\n+"))
                              #"#\+Title: " "-- ")
        ;; dbdesc (str/replace (first (str/split org #"(?m)^\* ")) #"#\+Title: " "")
        sections (rest (str/split org #"(?m)^\* "))
        ;; _ (pp/pprint sections), _ (println "\n\n")
        _ (check-duplicate-sections sections)
        comments (mapv process-section sections)]
    (when-not @roundtrip?
      (print-comments-file title comments))))

(defn usage []
  (println "DBDoc generates sql COMMENT statements from a dbdoc.org file.")
  (println "Run with no arguments to generate a migration file.")
  (println "Run with 'roundtrip' argument to print TSV for use with tsv2org script."))

(defn -main [& args]
  (let [[arg0 arg1] *command-line-args*]
    (if-not arg1
      (genmig)
      (if (or (= arg1 "roundtrip") (System/getenv "DBDOC_ROUNDTRIP"))
        (do (reset! roundtrip? true) (genmig))
        (do #_(println "arg1:" arg1) (usage))))))

;; (println "in dbdoc")
(-main)
