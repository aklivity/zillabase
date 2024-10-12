# Changelog

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
