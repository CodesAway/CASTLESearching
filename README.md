![CASTLE Searching](https://codesaway.info/images/CASTLESearching.png)

# CASTLE Searching
CASTLE Searching is an Eclipse plugin, which allows you to find what you're looking for faster.
https://codesaway.info/CASTLESearching/

## Update site
[codesaway.info/CASTLESearching/eclipse](https://codesaway.info/CASTLESearching/eclipse)

### Stable
* **Name**: CASTLE Searching
* **Location**: https://codesaway.info/CASTLESearching/eclipse
* **Version**: 1.2.4

### Beta
* **Name**: CASTLE Searching (beta)
* **Location**: https://codesaway.info/CASTLESearching/eclipse-beta
* **Version**: 1.5.0

## What is CASTLE Searching?
CASTLE Searching is a **C**ode **A**nalyzer **S**earch **T**ool **L**ooking @ **E**verything Searching

## How does it work?

In short, magic.

### Details
CASTLE Searching uses Apache Lucene to index your workspace files. It creates an index, like found in a book, so you can easily find related code based on searching by key phrases.

* Automatic reindexing of changed files (as changes are made within Eclipse or outside of Eclipse)
* Identifies key phases by breaking up camelCase, PascalCase, snake_case / SNAKE_CASE, and kebab-case phrases into their individual parts, such as "camel" and "case"
* Makes use of some Lucene functionality to search for the various word forms such as compare, compararing, comparison, and comparator.
* Indexes each line of code and stores metadata such as the Eclipse project, pathname, filename, extension, and line number
* Uses Lucene to handle case-insensitive user-specified "synonyms" / abbreviations and case-sensitive "acronyms"

This allows doing complex filtering with a simple query.

## Installation
1. Open Eclipse and under the **Help** menu, select **Install New Software...**
2. Click **Add...** to add a new site
   * Stable
      * **Name**: CASTLE Searching
      * **Location**: https://codesaway.info/CASTLESearching/eclipse
      * Click **Add**
   * Beta
      * **Name**: CASTLE Searching (beta)
      * **Location**: https://codesaway.info/CASTLESearching/eclipse-beta
      * Click **Add**
3. Check the box next to CASTLE Searching to install the plugin
4. Click **Next**
5. Accept the license agreement and click **Finish**
6. When prompted, install the plugin even though it's not digitally signed
7. Restart Eclipse when asked
8. Start CASTLE Searching! ![CASTLE Searching](https://codesaway.info/images/CASTLE.png)
   * In Eclipse, go to the search page (under the **Search** menu -> **Search...**) and CASTLE Searching will now be the first tab
   * You can also open the view under **Window** -> **Show View** -> **Other...**
   * This shows the CASTLE Searching view, where you can search and the results will be shown immediately
   * **NOTE**: The first time you run the plugin, it will index your workspace. This may take some time depending on the size of your code base, but you'll be able to start searching quickly even as it continues indexing.
