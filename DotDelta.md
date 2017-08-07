# Dot Delta File

Configuration for delta can be provided by an optional .delta file in
the root folder of the repository.

The first optional parameter specifies which branches of the project
delta should monitor. By default, delta monitors the 'master' branch.
Note that the branch names can be anything you like - there is no magic
naming conventions the delta expects (other than the default for master).

Additionally, there is per build configuration with the following supported

use cases:

   - Specifying what instance type to use in ECS
   - Supporting multiple projects in one build
   - Declaring dependencies - this allows the www build, for example,
     to not be scaled until after the api build is scaled
     successfully.
   - Build level settings, specify what is enabled or disabled. By default,
     all settings are enabled, but you can include an explicit "enable" to
     list the settings to enable, or a "disable" to specifically disable a 
     given setting. Available settings are documented at
     https://app.apibuilder.io/flow/delta/latest#model-settings

For projects with a single build, the default build is called 'root' and
does not need to be specified.

# Example File

Note a collection of valid configuration files is stored in
```lib/src/test/resources/config```


    stages:
      enable:
        - tag

    branches:
      - master
      - release

    builds:
      - api:
          dockerfile: api/Dockerfile
          instance.type: t2.micro
          memory: 3500
          initial.number.instances: 1
          port.container: 9000
          port.host: 6021
          disable:
            - scale
      - www:
          dockerfile: www/Dockerfile
          instance.type: t2.micro
          dependencies:
            - api
