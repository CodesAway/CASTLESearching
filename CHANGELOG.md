# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] - 2020-02-06
### Added
* View Toolbar toggle command to exclude comments from search results
  * Reqires rebuild of index to use new functionality
  * Can always opt to not reindex (will use the new format as files are modified)
* View Menu command to rebuild entire index
* New searchable field **comment** which will search through the comments, regardless if the exclude comments option is set or not
* View Toolbar commands have tooltips which mention the shortcut to toggle the command while focus is on the search box
* Automatically reload settings when the settings.xml is modified

## [1.3.0] - 2020-02-02
### Added
* View Toolbar toggle command to require each term by default
* Monitor settings file and reload file if changed
* View Toolbar toggle commands for each setting

### Changed
* Incremental Find changed from a confusing checkbox to a View Toolbar toggle command

## [1.2.4] - 2020-01-26
### Changed
* Various tweaks

## [1.2.0] - 2020-01-21
### Added
* Support for custom searchers (to allow searching already existing
* settings.xml file in plugin directory [workspace]\.metadata\.plugins\info.codesaway.castlesearching

## [1.1.0] - 2020-01-12
### Added
* Support for Incremental Find
  * Similar to Eclipse's functionality
  * Gives higher priority to matches found in the active file

## [1.0.1] - 2020-01-07
### Fixed
* Eclipse dependencies to not require a specific version (Eclipse defaulted to requiring the latest version)

## [1.0.0] - 2020-01-07
### Added
* Initial version
