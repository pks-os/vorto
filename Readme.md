[![Build Status](https://travis-ci.org/eclipse/vorto.svg?branch=development)](https://travis-ci.org/eclipse/vorto)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/569649bfe2594bedae2cd172e5ee0741)](https://www.codacy.com/app/alexander-edelmann/vorto?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=eclipse/vorto&amp;utm_campaign=Badge_Grade)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.eclipse.vorto/org.eclipse.vorto.parent/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.eclipse.vorto/org.eclipse.vorto.parent)

# Overview

[Vorto](http://www.eclipse.org/vorto) provides a Web Editor that lets you describe the device functionality and characteristics using a simple Vorto language (Vorto DSL) and save them as **Information Models**. These models are then managed in a [Vorto Repository](http://vorto.eclipse.org). [Code generators](http://vorto.eclipse.org/#/generators) convert these models into device - specific "stubs" that run on the device and send Information Model compliant messages to an IoT Backend. In order to process these messages in the IoT backend, Vorto offers a set of **technical components**, for example parsers and validators. For devices sending arbitrary, non-Vorto, messages to an IoT backend, the **Device Payload Mapping Engine** helps to transform these messages to platform-specific IoT Digital Twin API's, such as for Eclipse Ditto or AWS IoT Shadow.  
 
# Getting started with Vorto 

The easiest to get started, is to take a look at our [Getting Started Guide](https://www.eclipse.org/vorto/gettingstarted/)

# Developer Guide

## Repository Java Client

Search and access Vorto models as well as generate code using the [Repository Java Client](client/repository-java-client/Readme.md)

## Repository Import API

If you want to manage other existing (standardized) device descriptions with the Vorto Repository, you can extend the Repository by providing a model importer using the [Importer API](repository/repository-importer/Readme.md).

## Device Payload Transformation Engine

Map arbitrary device payload, expressed as JSON or binary, to standardized data, that is described by Vorto Information Models. See [Payload Mapping Documentation](https://www.eclipse.org/vorto/documentation/mappingengine) for more information. 

# Documentation

- Read our [tutorials](https://www.eclipse.org/vorto/tutorials/)
- Read our [Vorto Documentation](https://www.eclipse.org/vorto/documentation)

# Contact us
 - You want to chat with us ? [![Join the chat at https://gitter.im/eclipse/vorto](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/eclipse/vorto?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
 - You have problems with Vorto ? Open a [GitHub issue](https://github.com/eclipse/vorto/issues)
 - Find out more about the project on our [Vorto Homepage](http://www.eclipse.org/vorto)
 - Reach out to our developers on our [Discussion Forum](http://eclipse.org/forums/eclipse.vorto) 

# Contribute to the Project

When you create a Pull Request, make sure:

1. You have a valid CLA signed with Eclipse
2. All your commits are signed off (git commit -s)
3. Your commit message contains "Fixes #`<Github issue no>`
4. Target to merge your fix is development branch


