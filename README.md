# Minecraft Plugin Template

---

This repository serves as a template for developing
plugins for Minecraft. It includes useful helper scripts
for streamlining the version management as well as basic
setup code.

## Features
- Extract plugin information from the `plugin.yml` file
- Include a github workflow for publishing compiled releases
for different minecraft versions in the form of ready-to-use
jar files

## How to use
Fork this repository and give it a name. After cloning the
repository to your local machine, run the `bin/install.sh`
script to change parameters such as:
- plugin name
- group name
- spigot api version

After the modifications are done, the script automatically
applies the required changes and does an initial commit to
setup the project.

## Creating a release
To create a release for a specific Minecraft version, use
the `bin/release.sh` script. It will do the following steps:
- Prompt you for the Minecraft version you want to compile
the plugin for
- Change to a new branch for that version and make the
required adjustments to the files
- Build the project to check for any errors
- Create a commit with a tag that signals the github workflow
to create a release from that branch