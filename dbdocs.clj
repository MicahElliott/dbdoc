#!/usr/bin/env bb

;; Convert a simple ORG file into SQL COMMENTs for documenting tables
;; and their columns.

;; Default files for user to contigure
(def sql-file  (or (System/getenv "DBDOCS_SQL")  "resources/migrations/dbdocs.sql"))
(def org-file  (or (System/getenv "DBDOCS_ORG")  "docs/dbdocs.org"))
(def html-file (or (System/getenv "DBDOCS_HTML") "docs/dbdocs.html"))


;;; Translation

(defn check-dir [f]
  ;; (println "Checking for existence of directory:" f)
  (let [dir (.getParent (io/file f))]
    (when-not (.isDirectory (io/file dir))
      (println "ERROR:" dir "directory does not exist.")
      (System/exit 1))))

(defn process-col
  "Convert a column description into a SQL COMMENT.
  Construct a dotted table.column name, replace hyphens w/ underscores."
  [tab col]
  (let [cname (-> (str tab "." (str/replace col #"(?s)^- (.*) ::.*" "$1"))
                  (str/replace  #"-" "_"))
        text  (-> (str/join " " (rest (str/split col #"\n")))
                  str/trim
                  (str/replace  #"(?s)\s+" " ")
                  (str/replace  #"(?s)'" "’"))
        cmt   (format "COMMENT ON COLUMN %s IS '%s';" cname text)]
    cmt))

(defn process-section
  "Convert a section of table descriptions and combine with columns."
  [sec]
  (let [[tab tdesc & cols] (str/split sec #"\n\n")
        tab                (str/replace tab #"(?s)\n" " ")
        tdesc              (str/replace tdesc #"(?s)\n" " ")
        cmt                (format "COMMENT ON TABLE %s IS '%s';" tab tdesc)]
    (cons cmt (mapv #(process-col tab %) cols))))

(defn print-comments
  "Write all SQL comments to `sql-file`."
  [title comments]
  (binding [*out* (io/writer sql-file)]
    (println title)
    (doseq [c comments]
      (println)
      (doseq [i c]
        (println i)))))


;;; Main

;; To test, set DBDOCS_ORG=example.org
(println "Reading ORG descriptions input file:" org-file)
(def org (slurp org-file))
(def title
  (str/replace (first (str/split org #"\n\n+"))
               #"#\+Title: " "-- "))

(def sections (rest (str/split org #"(?m)^\* ")))

(def comments (mapv process-section sections))

(println "Writing SQL comments output file:" sql-file)
(print-comments title comments)
