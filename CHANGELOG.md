# Changelog

## [Unreleased](https://github.com/aklivity/zillabase/tree/HEAD)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.5.2...HEAD)

**Implemented enhancements:**

- Support raw output option when running query [\#194](https://github.com/aklivity/zillabase/issues/194)
- Aggregate types of Zview and Ztable into single column [\#192](https://github.com/aklivity/zillabase/issues/192)
- Implement Zillabase Studio Storage functionality [\#187](https://github.com/aklivity/zillabase/issues/187)
- Containerize Zillabase Studio and make it run as part of the stack [\#179](https://github.com/aklivity/zillabase/issues/179)
- Expose http endpoints to get java and python registered external udf functions [\#176](https://github.com/aklivity/zillabase/issues/176)
- Review/Refactor asyncapi and config generation [\#172](https://github.com/aklivity/zillabase/issues/172)
- zillabase quickstart example [\#158](https://github.com/aklivity/zillabase/issues/158)
- Support pgsql over websocket for admin service [\#139](https://github.com/aklivity/zillabase/issues/139)
- Review `CREATE STREAM` and `CREATE MATERIALIZED VIEW` syntax  [\#128](https://github.com/aklivity/zillabase/issues/128)
- Support selectively disabling unused compose services [\#127](https://github.com/aklivity/zillabase/issues/127)
- Support dynamic `asyncapi` specs as `sql` changes [\#126](https://github.com/aklivity/zillabase/issues/126)
- Support retention of state across `zillabase start` `zillabase stop` [\#124](https://github.com/aklivity/zillabase/issues/124)
- Support `zillabase migration apply` command [\#123](https://github.com/aklivity/zillabase/issues/123)
- Support `zillabase migration diff` command [\#122](https://github.com/aklivity/zillabase/issues/122)
- Support `/auth/sso/providers` local platform API [\#121](https://github.com/aklivity/zillabase/issues/121)
- Support `/auth/users` local platform API [\#120](https://github.com/aklivity/zillabase/issues/120)
- Support `/storage` local platform API [\#93](https://github.com/aklivity/zillabase/issues/93)

**Fixed bugs:**

- Storage file seems to be broken [\#197](https://github.com/aklivity/zillabase/issues/197)
- Search functionality only works on the specific page. [\#195](https://github.com/aklivity/zillabase/issues/195)

**Closed issues:**

- Storage delete functionality is not working [\#193](https://github.com/aklivity/zillabase/issues/193)
- Prepare documentation scaffolding [\#136](https://github.com/aklivity/zillabase/issues/136)

**Merged pull requests:**

- Update zilla version to 0.9.128 [\#209](https://github.com/aklivity/zillabase/pull/209) ([akrambek](https://github.com/akrambek))
- Error Message Fix [\#207](https://github.com/aklivity/zillabase/pull/207) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Fix table loading issues on changes and refactor code [\#206](https://github.com/aklivity/zillabase/pull/206) ([akrambek](https://github.com/akrambek))
- Minor fixes [\#205](https://github.com/aklivity/zillabase/pull/205) ([akrambek](https://github.com/akrambek))
- New logo [\#204](https://github.com/aklivity/zillabase/pull/204) ([akrambek](https://github.com/akrambek))
- Minor bug fixes and enhancements  [\#203](https://github.com/aklivity/zillabase/pull/203) ([akrambek](https://github.com/akrambek))
- Print result of executed query in table format [\#202](https://github.com/aklivity/zillabase/pull/202) ([akrambek](https://github.com/akrambek))
- Reset the pagination to the first page whenever a search query is entered [\#201](https://github.com/aklivity/zillabase/pull/201) ([akrambek](https://github.com/akrambek))
- Fix storage multi object deletion and add error message on none empty bucket deletion [\#200](https://github.com/aklivity/zillabase/pull/200) ([akrambek](https://github.com/akrambek))
- Aggregate types into single table column [\#199](https://github.com/aklivity/zillabase/pull/199) ([akrambek](https://github.com/akrambek))
- Fix uploading file to storage [\#198](https://github.com/aklivity/zillabase/pull/198) ([akrambek](https://github.com/akrambek))
- Update zilla version [\#191](https://github.com/aklivity/zillabase/pull/191) ([akrambek](https://github.com/akrambek))
- Provider Changes [\#190](https://github.com/aklivity/zillabase/pull/190) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Storage UI Fixes [\#189](https://github.com/aklivity/zillabase/pull/189) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Bump dompurify and swagger-ui in /studio [\#188](https://github.com/aklivity/zillabase/pull/188) ([dependabot[bot]](https://github.com/apps/dependabot))
- Dockerize zillabase studio [\#186](https://github.com/aklivity/zillabase/pull/186) ([akrambek](https://github.com/akrambek))
- External Function Changes [\#185](https://github.com/aklivity/zillabase/pull/185) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Refactor asyncapi spec/config generation [\#184](https://github.com/aklivity/zillabase/pull/184) ([akrambek](https://github.com/akrambek))
- Bump vite and @quasar/app-vite in /studio [\#183](https://github.com/aklivity/zillabase/pull/183) ([dependabot[bot]](https://github.com/apps/dependabot))
- Bump serialize-javascript from 6.0.1 to 6.0.2 in /examples/streampay [\#182](https://github.com/aklivity/zillabase/pull/182) ([dependabot[bot]](https://github.com/apps/dependabot))
- Bump nanoid from 3.3.7 to 3.3.8 in /studio [\#181](https://github.com/aklivity/zillabase/pull/181) ([dependabot[bot]](https://github.com/apps/dependabot))
- Bump path-to-regexp and express in /studio [\#180](https://github.com/aklivity/zillabase/pull/180) ([dependabot[bot]](https://github.com/apps/dependabot))
- Expose udf methods via api [\#177](https://github.com/aklivity/zillabase/pull/177) ([akrambek](https://github.com/akrambek))
- Support zillabase migration diff and apply commands [\#175](https://github.com/aklivity/zillabase/pull/175) ([akrambek](https://github.com/akrambek))
- Update risingwave routing config [\#174](https://github.com/aklivity/zillabase/pull/174) ([akrambek](https://github.com/akrambek))
- Support new zfunction format [\#171](https://github.com/aklivity/zillabase/pull/171) ([akrambek](https://github.com/akrambek))
- Update zilla version [\#170](https://github.com/aklivity/zillabase/pull/170) ([akrambek](https://github.com/akrambek))
- Support dynamic asyncapi specs as sql changes [\#169](https://github.com/aklivity/zillabase/pull/169) ([akrambek](https://github.com/akrambek))
- Enable CORS in Apicurio [\#168](https://github.com/aklivity/zillabase/pull/168) ([akrambek](https://github.com/akrambek))
- Make use of zfunction and zstream in streampay [\#167](https://github.com/aklivity/zillabase/pull/167) ([akrambek](https://github.com/akrambek))
- Async API Studio Added [\#165](https://github.com/aklivity/zillabase/pull/165) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Connection added with PGSQL, APIs Fixes [\#164](https://github.com/aklivity/zillabase/pull/164) ([Tafseerhussain](https://github.com/Tafseerhussain))
- quickstart example [\#163](https://github.com/aklivity/zillabase/pull/163) ([ankitk-me](https://github.com/ankitk-me))
- Websocket Message Fix [\#162](https://github.com/aklivity/zillabase/pull/162) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Enable http cross-origin policy for admin service [\#161](https://github.com/aklivity/zillabase/pull/161) ([jfallows](https://github.com/jfallows))
- Storage APIs Added, WS connection changed [\#159](https://github.com/aklivity/zillabase/pull/159) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Tidy `zillabase` services [\#157](https://github.com/aklivity/zillabase/pull/157) ([jfallows](https://github.com/jfallows))
- Update migration to use ZVIEW and ZTABLE [\#156](https://github.com/aklivity/zillabase/pull/156) ([akrambek](https://github.com/akrambek))
- Bootstrap Risingwave `zb_catalog` [\#155](https://github.com/aklivity/zillabase/pull/155) ([jfallows](https://github.com/jfallows))
- Build via GitHub Actions [\#154](https://github.com/aklivity/zillabase/pull/154) ([jfallows](https://github.com/jfallows))
- Bump nanoid from 3.3.7 to 3.3.8 in /examples/streampay [\#153](https://github.com/aklivity/zillabase/pull/153) ([dependabot[bot]](https://github.com/apps/dependabot))
- selectively disabling unused compose services [\#152](https://github.com/aklivity/zillabase/pull/152) ([ankitk-me](https://github.com/ankitk-me))
- update `config list` command to `config show` [\#151](https://github.com/aklivity/zillabase/pull/151) ([ankitk-me](https://github.com/ankitk-me))
- Bump path-to-regexp and express in /examples/streampay [\#150](https://github.com/aklivity/zillabase/pull/150) ([dependabot[bot]](https://github.com/apps/dependabot))
- Added Zillabase Studio in Vue.js with Quasar [\#147](https://github.com/aklivity/zillabase/pull/147) ([Tafseerhussain](https://github.com/Tafseerhussain))
- Bump cross-spawn and @quasar/app-webpack in /examples/streampay [\#145](https://github.com/aklivity/zillabase/pull/145) ([dependabot[bot]](https://github.com/apps/dependabot))
- update `asyncapi` command to support `read-only` [\#143](https://github.com/aklivity/zillabase/pull/143) ([ankitk-me](https://github.com/ankitk-me))
- Bump org.apache.kafka:kafka-clients from 2.6.3 to 3.7.1 [\#142](https://github.com/aklivity/zillabase/pull/142) ([dependabot[bot]](https://github.com/apps/dependabot))
- Support retention of state across `zillabase` [\#141](https://github.com/aklivity/zillabase/pull/141) ([ankitk-me](https://github.com/ankitk-me))
- Support pgsql over websocket for admin service [\#140](https://github.com/aklivity/zillabase/pull/140) ([ankitk-me](https://github.com/ankitk-me))
- `/auth/sso/providers` local platform api [\#138](https://github.com/aklivity/zillabase/pull/138) ([ankitk-me](https://github.com/ankitk-me))
- `/storage` local platform API [\#132](https://github.com/aklivity/zillabase/pull/132) ([ankitk-me](https://github.com/ankitk-me))
- `/auth/users` local platform API implementation [\#131](https://github.com/aklivity/zillabase/pull/131) ([ankitk-me](https://github.com/ankitk-me))
- Bump http-proxy-middleware from 2.0.6 to 2.0.7 in /examples/streampay [\#119](https://github.com/aklivity/zillabase/pull/119) ([dependabot[bot]](https://github.com/apps/dependabot))
- Add `auth` & `storage` api defination [\#118](https://github.com/aklivity/zillabase/pull/118) ([ankitk-me](https://github.com/ankitk-me))

## [0.5.2](https://github.com/aklivity/zillabase/tree/0.5.2) (2024-12-03)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.5.1...0.5.2)

**Merged pull requests:**

- Upgrade zilla version [\#149](https://github.com/aklivity/zillabase/pull/149) ([jfallows](https://github.com/jfallows))
- Fix minor issues in streampay UI [\#148](https://github.com/aklivity/zillabase/pull/148) ([akrambek](https://github.com/akrambek))

## [0.5.1](https://github.com/aklivity/zillabase/tree/0.5.1) (2024-12-02)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.5.0...0.5.1)

**Merged pull requests:**

- Upgrade zilla version [\#146](https://github.com/aklivity/zillabase/pull/146) ([jfallows](https://github.com/jfallows))

## [0.5.0](https://github.com/aklivity/zillabase/tree/0.5.0) (2024-12-01)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.4.0...0.5.0)

**Closed issues:**

- Update schemas to catalog z related queries into a separate schema and make  `public` schema as default [\#137](https://github.com/aklivity/zillabase/issues/137)

**Merged pull requests:**

- Upgrade zilla and openapi versions [\#144](https://github.com/aklivity/zillabase/pull/144) ([akrambek](https://github.com/akrambek))
- Update risingwave docker image version [\#134](https://github.com/aklivity/zillabase/pull/134) ([akrambek](https://github.com/akrambek))
- Update sql queries to follow new include format [\#117](https://github.com/aklivity/zillabase/pull/117) ([akrambek](https://github.com/akrambek))
- indentation fix for config in readme file [\#116](https://github.com/aklivity/zillabase/pull/116) ([ankitk-me](https://github.com/ankitk-me))
- Fix request payment accept and reject functionality [\#115](https://github.com/aklivity/zillabase/pull/115) ([akrambek](https://github.com/akrambek))

## [0.4.0](https://github.com/aklivity/zillabase/tree/0.4.0) (2024-10-12)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.3.0...0.4.0)

**Implemented enhancements:**

- Support sql migrations [\#101](https://github.com/aklivity/zillabase/issues/101)
- support installing python dependencies from a `requirements.txt` file [\#94](https://github.com/aklivity/zillabase/issues/94)

**Merged pull requests:**

- Show help by default for command groups and when command parsing fails [\#114](https://github.com/aklivity/zillabase/pull/114) ([jfallows](https://github.com/jfallows))
- Decrease polling interval [\#113](https://github.com/aklivity/zillabase/pull/113) ([akrambek](https://github.com/akrambek))
- Update zillabase start output to convey when containers are healthy [\#112](https://github.com/aklivity/zillabase/pull/112) ([jfallows](https://github.com/jfallows))
- Use migrations for functions examples [\#111](https://github.com/aklivity/zillabase/pull/111) ([jfallows](https://github.com/jfallows))
- Separate streampay seed.sql into sql migrations [\#110](https://github.com/aklivity/zillabase/pull/110) ([jfallows](https://github.com/jfallows))
- Remove apexchart from Vue boot [\#109](https://github.com/aklivity/zillabase/pull/109) ([jfallows](https://github.com/jfallows))
- Support sql migrations [\#108](https://github.com/aklivity/zillabase/pull/108) ([jfallows](https://github.com/jfallows))
- Remove statement page and fix activities logs on main page [\#107](https://github.com/aklivity/zillabase/pull/107) ([akrambek](https://github.com/akrambek))
- petstore example update to use seed.sql [\#106](https://github.com/aklivity/zillabase/pull/106) ([ankitk-me](https://github.com/ankitk-me))
- fix: add more users and UI instructions [\#105](https://github.com/aklivity/zillabase/pull/105) ([vordimous](https://github.com/vordimous))
- Cli and logging [\#104](https://github.com/aklivity/zillabase/pull/104) ([ankitk-me](https://github.com/ankitk-me))
- feat: add OpenAI and AsyncAPI instructions [\#103](https://github.com/aklivity/zillabase/pull/103) ([vordimous](https://github.com/vordimous))
- Streampay with OpenAI integration [\#100](https://github.com/aklivity/zillabase/pull/100) ([vordimous](https://github.com/vordimous))
- fix: try connection multiple times and execute sql once [\#99](https://github.com/aklivity/zillabase/pull/99) ([vordimous](https://github.com/vordimous))
- support installing python dependencies from a requirements.txt file [\#98](https://github.com/aklivity/zillabase/pull/98) ([ankitk-me](https://github.com/ankitk-me))
- Support default image version tags, overridable image version tags and function env vars [\#97](https://github.com/aklivity/zillabase/pull/97) ([jfallows](https://github.com/jfallows))
- fix: update node, fix build warnings and lint errors [\#96](https://github.com/aklivity/zillabase/pull/96) ([vordimous](https://github.com/vordimous))
- Set docker restart policy to unless-stopped for each container [\#95](https://github.com/aklivity/zillabase/pull/95) ([jfallows](https://github.com/jfallows))
- fix: remove dollar prefix and folder spelling [\#92](https://github.com/aklivity/zillabase/pull/92) ([vordimous](https://github.com/vordimous))

## [0.3.0](https://github.com/aklivity/zillabase/tree/0.3.0) (2024-10-07)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.2.0...0.3.0)

**Implemented enhancements:**

- Support user-defined `functions` written in `Python` [\#76](https://github.com/aklivity/zillabase/issues/76)

**Merged pull requests:**

- udf server - python example [\#91](https://github.com/aklivity/zillabase/pull/91) ([ankitk-me](https://github.com/ankitk-me))
- udf server - python implementation [\#90](https://github.com/aklivity/zillabase/pull/90) ([ankitk-me](https://github.com/ankitk-me))
- Seed users with initial balance [\#89](https://github.com/aklivity/zillabase/pull/89) ([akrambek](https://github.com/akrambek))

## [0.2.0](https://github.com/aklivity/zillabase/tree/0.2.0) (2024-10-03)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.1.0...0.2.0)

**Implemented enhancements:**

- Support `brew install zillabase` [\#38](https://github.com/aklivity/zillabase/issues/38)
- Migrate `zilla streampay demo` to `zillabase` guided steps [\#1](https://github.com/aklivity/zillabase/issues/1)

**Merged pull requests:**

- Streampay example [\#79](https://github.com/aklivity/zillabase/pull/79) ([jfallows](https://github.com/jfallows))

## [0.1.0](https://github.com/aklivity/zillabase/tree/0.1.0) (2024-09-19)

[Full Changelog](https://github.com/aklivity/zillabase/compare/0.0.1...0.1.0)

## [0.0.1](https://github.com/aklivity/zillabase/tree/0.0.1) (2024-09-17)

[Full Changelog](https://github.com/aklivity/zillabase/compare/500f474c76783292c8848b124cf81831dfb36440...0.0.1)

**Implemented enhancements:**

- Use `karapace` as `schema-registry` for Kafka schemas [\#50](https://github.com/aklivity/zillabase/issues/50)
- Support user-defined `functions` written in `Java` [\#49](https://github.com/aklivity/zillabase/issues/49)
- `zillabase start` should proxy `pgsql` protocol via `zillabase admin service` [\#48](https://github.com/aklivity/zillabase/issues/48)
- `zillabase` `config` command [\#41](https://github.com/aklivity/zillabase/issues/41)
- Configure `keycloak` on `zillabase start` [\#37](https://github.com/aklivity/zillabase/issues/37)
- Generate AsyncAPI http specification with access control [\#36](https://github.com/aklivity/zillabase/issues/36)
- `zillabase start` should process kafka-seed.yaml [\#31](https://github.com/aklivity/zillabase/issues/31)
- Design `functions` integration [\#25](https://github.com/aklivity/zillabase/issues/25)
- `zillabase start` should Include `keycloak` service [\#24](https://github.com/aklivity/zillabase/issues/24)
- Design security integration [\#23](https://github.com/aklivity/zillabase/issues/23)
- `zillabase start` should deploy `zilla.yaml` [\#22](https://github.com/aklivity/zillabase/issues/22)
- `zillabase start` should include `config` service [\#21](https://github.com/aklivity/zillabase/issues/21)
- `zillabase start` should generate default AsyncAPI specs [\#20](https://github.com/aklivity/zillabase/issues/20)
- `zillabase start` should process `seed.sql` [\#19](https://github.com/aklivity/zillabase/issues/19)
- `zillabase start` should Include `admin` service [\#18](https://github.com/aklivity/zillabase/issues/18)
- Support docker compose stack for `zillabase start` and `zillabase stop` [\#17](https://github.com/aklivity/zillabase/issues/17)
- `zillabase` `asyncapi` command [\#12](https://github.com/aklivity/zillabase/issues/12)
- Implement `zillabase` admin REST API v1 [\#7](https://github.com/aklivity/zillabase/issues/7)
- Design `zillabase` admin REST API v1 [\#6](https://github.com/aklivity/zillabase/issues/6)
- `zillabase` `sso` command [\#5](https://github.com/aklivity/zillabase/issues/5)
- `zillabase` `stop` command [\#4](https://github.com/aklivity/zillabase/issues/4)
- `zillabase` `start` command [\#3](https://github.com/aklivity/zillabase/issues/3)
- `zillabase` `init` command [\#2](https://github.com/aklivity/zillabase/issues/2)

**Merged pull requests:**

- udf support & udf-server implementation [\#54](https://github.com/aklivity/zillabase/pull/54) ([ankitk-me](https://github.com/ankitk-me))
- updated zillabase admin service using zilla [\#53](https://github.com/aklivity/zillabase/pull/53) ([ankitk-me](https://github.com/ankitk-me))
- fix schema type issue for Karapace [\#52](https://github.com/aklivity/zillabase/pull/52) ([ankitk-me](https://github.com/ankitk-me))
- Use karapace as schema-registry for Kafka schemas [\#51](https://github.com/aklivity/zillabase/pull/51) ([ankitk-me](https://github.com/ankitk-me))
- generate http asyncapi spec & zilla.yaml with JWT guard [\#47](https://github.com/aklivity/zillabase/pull/47) ([ankitk-me](https://github.com/ankitk-me))
- `zillabase sso` command implementation [\#46](https://github.com/aklivity/zillabase/pull/46) ([ankitk-me](https://github.com/ankitk-me))
- Configure `keycloak` on `zillabase start` [\#45](https://github.com/aklivity/zillabase/pull/45) ([ankitk-me](https://github.com/ankitk-me))
- Support image tag config for zilla and admin services [\#44](https://github.com/aklivity/zillabase/pull/44) ([jfallows](https://github.com/jfallows))
- Petstore example using zillabase [\#43](https://github.com/aklivity/zillabase/pull/43) ([ankitk-me](https://github.com/ankitk-me))
- zillabase config command implementation [\#42](https://github.com/aklivity/zillabase/pull/42) ([ankitk-me](https://github.com/ankitk-me))
- Zillabase Config Schema Validation [\#40](https://github.com/aklivity/zillabase/pull/40) ([ankitk-me](https://github.com/ankitk-me))
- Publish zilla.yaml to config server and use for zilla service start [\#35](https://github.com/aklivity/zillabase/pull/35) ([ankitk-me](https://github.com/ankitk-me))
- Bump org.apache.kafka:kafka-clients from 2.3.0 to 2.6.3 [\#34](https://github.com/aklivity/zillabase/pull/34) ([dependabot[bot]](https://github.com/apps/dependabot))
- `zillabase start` starts `config` service [\#33](https://github.com/aklivity/zillabase/pull/33) ([ankitk-me](https://github.com/ankitk-me))
- zillabase start process kafka-seed.yaml [\#32](https://github.com/aklivity/zillabase/pull/32) ([ankitk-me](https://github.com/ankitk-me))
- Process `seed.sql` on `zillabase start` [\#29](https://github.com/aklivity/zillabase/pull/29) ([ankitk-me](https://github.com/ankitk-me))
- `zillabase start` include admin `service` [\#28](https://github.com/aklivity/zillabase/pull/28) ([ankitk-me](https://github.com/ankitk-me))
- Include keycloak service [\#27](https://github.com/aklivity/zillabase/pull/27) ([jfallows](https://github.com/jfallows))
- Docker compose stack [\#26](https://github.com/aklivity/zillabase/pull/26) ([jfallows](https://github.com/jfallows))
- zillabase admin REST API v1 implementation [\#15](https://github.com/aklivity/zillabase/pull/15) ([ankitk-me](https://github.com/ankitk-me))
- `zillabase asyncapi` command implementation [\#14](https://github.com/aklivity/zillabase/pull/14) ([ankitk-me](https://github.com/ankitk-me))
- Initial admin REST API definition [\#13](https://github.com/aklivity/zillabase/pull/13) ([jfallows](https://github.com/jfallows))
- Initial implementation of stop command [\#11](https://github.com/aklivity/zillabase/pull/11) ([jfallows](https://github.com/jfallows))
- Initial implementation of start command [\#10](https://github.com/aklivity/zillabase/pull/10) ([jfallows](https://github.com/jfallows))
- Implement zillabase init command [\#9](https://github.com/aklivity/zillabase/pull/9) ([jfallows](https://github.com/jfallows))
- Initial zillabase cli [\#8](https://github.com/aklivity/zillabase/pull/8) ([jfallows](https://github.com/jfallows))



\* *This Changelog was automatically generated by [github_changelog_generator](https://github.com/github-changelog-generator/github-changelog-generator)*
