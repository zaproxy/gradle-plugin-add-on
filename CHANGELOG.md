# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Fixed
 - Update the copy destination in help information for the copyZapAddOn task.

## [0.3.0] - 2020-01-14
### Added
 - Allow to specify the location of the repo of the add-on. For example:
   ```kts
   manifest {
     repo.set("https://github.com/zaproxy/zap-hud/tree/develop/")
   }
   ```
 - Allow to bundle dependencies (instead of the add-on being an uber JAR), to properly maintain/access all
 dependencies' JAR data (e.g. module info, manifest, services). For example, to bundle the runtime dependencies:
   ```kts
   manifest {
     bundledLibs {
       libs.from(configurations.runtimeClasspath)
    }
   }
   ```

### Removed
 - Remove features related to the wiki generation, the help will be converted when releasing the
 add-on to the marketplace. The following tasks and extensions will no longer be available:
   - Tasks added:
     - `generateWiki-*`
     - `copyWiki-*`
   - Task provided:
     - `org.zaproxy.gradle.addon.wiki.tasks.GenerateWiki`
   - Extension:
     - `wikiGen` (in `zapAddOn`)

## [0.2.0] - 2019-05-20
### Added
Add (opinionated) tasks and extension properties to help with the release of the add-on.
It makes use of a changelog, in Keep a Changelog format.

#### Extension
 - Add the following properties to `zapAddOn` extension:
   - `changelog` to configure the location of the changelog (defaults to `CHANGELOG.md`).
   - `releaseLink` to configure the release link added to the changelog when preparing the release.
   - `unreleasedLink` to configure the unreleased link added to the changelog when preparing the next development
   iteration.

#### Tasks
Added by the plugin:
 - `extractLatestChanges` - to extract the latest changes from the changelog, kept in markdown.
 - `generateManifestChanges` - to generate the manifest changes, by converting the latest changes to HTML. Needs to be
 set to the `changesFile` of the `manifest`.
 - `prepareAddOnRelease` - to prepare the release of the add-on.
 - `prepareAddOnNextDevIter` - to prepare the next development iteration of the add-on.

Provided by the plugin:
 - `org.zaproxy.gradle.addon.misc.ConvertMarkdownToHtml` - converts markdown into HTML, for the add-on manifest.
 - `org.zaproxy.gradle.addon.misc.CreateGitHubRelease` - creates a GitHub release.
 - `org.zaproxy.gradle.addon.misc.ExtractLatestChangesFromChangelog` - extracts the changes from the
 latest release, for example, to be included in the GitHub release or add-on manifest (after converting to HTML).
 - `org.zaproxy.gradle.addon.misc.PrepareAddOnNextDevIter` - prepares the next development iteration
 of the add-on. Updates the changelog (with Unreleased section) and bumps the version of the add-on.
 - `org.zaproxy.gradle.addon.misc.PrepareAddOnRelease` - prepares the release of the add-on. Replaces the
 Unreleased section of the changelog with the version, release date, and link to the release.

### Changed
 - The `installZapAddOn` task will no longer depend on `uninstallZapAddOn` task, the uninstall will be
 performed by ZAP to not require the uninstallation of dependent add-ons.
 - The `jarZapAddOn` task no longer must run after `uninstallZapAddOn` task, ZAP now copies the add-on
 to local plugin directory so the add-on built should no longer conflict with the one being uninstalled.
 - Update default path of `CopyAddOn` task to the new location in zaproxy project (from
 `$rootDir/../zaproxy/src/plugin/` to `$rootDir/../zaproxy/zap/src/main/dist/plugin/`).
 - Update dependencies.

### Fixed
- Fix wiki generation on Windows, which failed to find the help files.

### Removed
 - Remove features related to `ZapVersions.xml` file, the data will be generated in/by
 the repository containing the marketplace data. The following tasks and extensions will
 no longer be available:
   - Task added:
     - `generateZapVersionsFile`
   - Tasks provided:
     - `org.zaproxy.gradle.addon.zapversions.tasks.AggregateZapVersionsFiles`
     - `org.zaproxy.gradle.addon.zapversions.tasks.GenerateZapVersionsFile`
     - `org.zaproxy.gradle.addon.zapversions.tasks.UpdateZapVersionsFile`
   - Extension:
     - `zapVersions` (in `zapAddOn`)
 - Remove the task `org.zaproxy.gradle.addon.manifest.tasks.ConvertChangelogToChanges`,
 split into two tasks (`ExtractLatestChangesFromChangelog` and `ConvertMarkdownToHtml`).

## [0.1.0] - 2019-03-11
First alpha release.

### Added
A plugin with ID `org.zaproxy.add-on` applied after `JavaPlugin` thus allowing to build ZAP
add-ons written (at least) in Java, Groovy, or Kotlin.

The plugin provides the following features:

#### Extension
 - `zapAddOn` to configure the ID, name, status, version, and target ZAP version of the add-on. It also
 provides the following extensions:
   - `manifest` to configure the generated add-on manifest (`ZapAddOn.xml` file).
   - `zapVersions` to configure the generated `ZapVersions.xml` file (metadata for release to ZAP
   marketplace).
   - `apiClientGen` to configure the generated ZAP API client files.
   - `wikiGen` to configure the generation of the wiki files.

#### Conventions
 - The add-on help, in JavaHelp format, will be read from `src/main/javahelp/`.
 - Files to be deployed to ZAP home directory are read from `src/main/zapHomeFiles/`, automatically
 added to main resources and declared in the add-on manifest.

#### Configurations
 - `zap` - the ZAP dependency, automatically added to `compileOnly` and `testImplementation`. Defaults
 to `org.zaproxy:zap` with the ZAP version specified in the `zapAddOn` extension.
 - `javahelp` - the dependencies to use with JavaHelp indexer. Defaults to `javax.help:javahelp:2.0.05`.

#### Tasks
Added by the plugin:
 - `generateZapAddOnManifest` - to generate the manifest file for the add-on, added to `jar` task.
 - `generateZapApiClientFiles` - to generate the ZAP API client files.
 - `generateZapVersionsFile` - to generate the `ZapVersion.xml` file.
 - `jarZapAddOn` - to assemble the add-on (dependency of `assemble` task), it includes the default `jar`,
 the `runtimeClasspath`, and the help files. Placed in the directory `build/zapAddOn/bin/`.
 - `jhindexer-*` - to build the JavaHelp indexes, one for each `HelpSet` in `src/main/javahelp/`.
 - `generateWiki-*` - to generate the wiki files, one for each `HelpSet` in `src/main/javahelp/`.
 - `copyWiki-*` - to copy the generated wiki files to a directory, one for each `HelpSet` in `src/main/javahelp/`.
 - `copyZapAddOn` - to copy the add-on to zaproxy project.
 - `deployZapAddOn` - to deploy the add-on and its home files to ZAP home dir.
 - `installZapAddOn` - to install the add-on into ZAP.
 - `uninstallZapAddOn` - to uninstall the add-on from ZAP.

Provided by the plugin:
 - `org.zaproxy.gradle.addon.apigen.tasks.GenerateApiClientFiles` - generates the ZAP API client files.
 - `org.zaproxy.gradle.addon.jh.tasks.JavaHelpIndexer` - invokes `jhindexer` for a given collection
 of files.
 - `org.zaproxy.gradle.addon.manifest.tasks.ConvertChangelogToChanges` - converts a changelog (in
 Keep a Changelog format) to manifest changes (HTML).
 - `org.zaproxy.gradle.addon.manifest.tasks.GenerateManifestFile` - generates the add-on manifest.
 - `org.zaproxy.gradle.addon.misc.CopyAddOn` - copies the add-on (and deletes old ones), defaults to zaproxy project.
 - `org.zaproxy.gradle.addon.misc.DeployAddOn` - deploys the add-on and its home files to ZAP home dir.
 - `org.zaproxy.gradle.addon.misc.InstallAddOn` - installs the add-on into ZAP.
 - `org.zaproxy.gradle.addon.misc.UninstallAddOn` - uninstalls the add-on from ZAP.
 - `org.zaproxy.gradle.addon.wiki.tasks.GenerateWiki` - generates the wiki files from help files in a JAR.
 - `org.zaproxy.gradle.addon.zapversions.tasks.AggregateZapVersionsFiles` - merges `ZapVersions.xml`
 from add-ons into a single file.
 - `org.zaproxy.gradle.addon.zapversions.tasks.GenerateZapVersionsFile` - generates a `ZapVersions.xml`
 file for a given add-on.
 - `org.zaproxy.gradle.addon.zapversions.tasks.UpdateZapVersionsFile` - updates `ZapVersions.xml` files
 with a `ZapVersions.xml` from an add-on.


[Unreleased]: https://github.com/zaproxy/gradle-plugin-add-on/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/zaproxy/gradle-plugin-add-on/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/zaproxy/gradle-plugin-add-on/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/zaproxy/gradle-plugin-add-on/compare/47fb1005b5362df23bbe0aadf1935755db0dc811...v0.1.0
