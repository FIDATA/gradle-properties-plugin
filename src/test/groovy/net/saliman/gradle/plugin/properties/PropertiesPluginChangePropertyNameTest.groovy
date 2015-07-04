/*
 * Copyright 2012-2015 Steven C. Saliman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package net.saliman.gradle.plugin.properties

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

/**
 * The {@link PropertiesPluginParentProjectTest} class tests applying the
 * plugin with the standard {@code environmentName} and {@code gradleUserName}
 * properties driving the environment name and gradle user.
 * <p>
 * This class tests changing these properties via
 * {@code propertiesPluginEnvironmentNameProperty} and
 * {@code propertiesPluginGradleUserNameProperty} properties before applying
 * the plugin to make sure we still do the right thing when the user wants to
 * reconfigure what the plugin uses during apply time.
 *
 * @author Steven C. Saliman
 */
class PropertiesPluginChangePropertyNameTest extends GroovyTestCase {
	def parentProject = null;
	def childProject = null;
	def parentCommandProperties = null;
	def childCommandProperties = null;

	/**
	 * Set up the test data.  This calls a helper method to create the projects
	 * because we need to repeat the setup in one of the tests.
	 */
	public void setUp() {
		createProjects(true)
		copyFiles()
	}

	/**
	 * Set up for each test. This will create a parent project with one child
	 * project, and it will set the properties that would be set before the plugin
	 * is applied.  These properties would be set by Gradle itself as it creates
	 * projects from build.gradle files.  Gradle only handles properties in the
	 * command line and the 3 gradle.properties file.
	 */
	public void createProjects(includeProjectProperties) {
		// Create the parent project.
		def parentProjectDir = new File('build/test/parentProject')
		parentProject = ProjectBuilder
						.builder()
						.withName('parentProject')
						.withProjectDir(parentProjectDir)
						.build();
		if ( includeProjectProperties ) {
			parentProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
		}
		parentProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
		parentProject.ext.childProjectProperty = 'ParentProject.childProjectValue'
		parentProject.ext.childEnvironmentProperty = 'ParentProject.childEnvironmentValue'
		parentProject.ext.homeProperty = 'Home.homeValue'
		parentProject.ext.userProperty = 'Home.userValue'
		parentCommandProperties = parentProject.gradle.startParameter.projectProperties

		// Create the child project.
		def childProjectDir = new File('build/test/parentProject/childProject')
		childProject = ProjectBuilder
						.builder()
						.withName('childProject')
						.withParent(parentProject)
						.withProjectDir(childProjectDir)
						.build();
		if ( includeProjectProperties ) {
			childProject.ext.parentProjectProperty = 'ParentProject.parentProjectValue'
		}
		childProject.ext.parentEnvironmentProperty = 'ParentProject.parentEnvironmentValue'
		childProject.ext.childProjectProperty = 'ChildProject.childProjectValue'
		childProject.ext.childEnvironmentProperty = 'ChildProject.childEnvironmentValue'
		childProject.ext.homeProperty = 'Home.homeValue'
		childProject.ext.userProperty = 'Home.userValue'
		childCommandProperties = parentProject.gradle.startParameter.projectProperties

		// Add the child to the parent.
		parentProject.childProjects.put('child', childProject)
	}

	/**
	 * Do the actual work of copying the test property files to the correct
	 * locations within the project hierarchy
	 */
	def copyFiles() {
		// Copy the files for the parent project.
		def parentUserDir = parentProject.gradle.gradleUserHomeDir
		def builder = new AntBuilder()
		builder.copy(file:'src/test/resources/parent-project-gradle.properties',
						tofile : "${parentProject.projectDir}/gradle.properties")
		builder.copy(file:'src/test/resources/parent-env-test.properties',
						tofile : "${parentProject.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/parent-env-local.properties',
						tofile : "${parentProject.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/home-gradle.properties',
						tofile : "${parentUserDir}/gradle.properties")
		builder.copy(file:'src/test/resources/user-gradle.properties',
						tofile : "${parentUserDir}/gradle-user.properties")
		builder.copy(file:'src/test/resources/parent-env-local-sub.properties',
						tofile : "${parentProject.projectDir}/gradle-properties/gradle-local.properties")

		// copy the files for the child project.
		def childUserDir = childProject.gradle.gradleUserHomeDir
		builder.copy(file:'src/test/resources/child-project-gradle.properties',
						tofile : "${childProject.projectDir}/gradle.properties")
		builder.copy(file:'src/test/resources/child-env-test.properties',
						tofile : "${childProject.projectDir}/gradle-test.properties")
		builder.copy(file:'src/test/resources/child-env-local.properties',
						tofile : "${childProject.projectDir}/gradle-local.properties")
		builder.copy(file:'src/test/resources/home-gradle.properties',
						tofile : "${childUserDir}/gradle.properties")
		builder.copy(file:'src/test/resources/user-gradle.properties',
						tofile : "${childUserDir}/gradle-user.properties")
		builder.copy(file:'src/test/resources/child-env-local-sub.properties',
						tofile : "${childProject.projectDir}/gradle-properties/gradle-local.properties")
	}

	/**
	 * Helper method to set the properties that Gradle would have set for us
	 * via the command line, system properties, and environment variables.
	 * @param setEnvironmentProperties whether or not to set the environment
	 *        properties
	 * @param setSystemProperties whether or not to set the system properties
	 * @param commandProperties a map of properties that should be added to the
	 *        "command line" for our build.
	 */
	public void setNonFileProperties(boolean setEnvironmentProperties,
	                                 boolean setSystemProperties,
	                                 Map commandProperties) {
		if ( setEnvironmentProperties ) {
			SetEnv.setEnv([ 'ORG_GRADLE_PROJECT_environmentProperty' : 'Environment.environmentValue',
							'ORG_GRADLE_PROJECT_systemProperty' : 'Environment.systemValue',
							'ORG_GRADLE_PROJECT_commandProperty' :'Environment.commandValue'])
			// Make sure the utility worked.
			assertEquals('Failed to set ORG_GRADLE_PROJECT_environmentProperty',
							'Environment.environmentValue',
							System.getenv('ORG_GRADLE_PROJECT_environmentProperty'))
			assertEquals('Failed to set ORG_GRADLE_PROJECT_systemProperty',
							'Environment.systemValue',
							System.getenv('ORG_GRADLE_PROJECT_systemProperty'))
			assertEquals('Failed to set ORG_GRADLE_PROJECT_commandProperty',
							'Environment.commandValue',
							System.getenv('ORG_GRADLE_PROJECT_commandProperty'))
		} else {
			SetEnv.unsetEnv(['ORG_GRADLE_PROJECT_environmentProperty',
							'ORG_GRADLE_PROJECT_systemProperty',
							'ORG_GRADLE_PROJECT_commandProperty'])
			// Make sure the utility worked.
			assertNull('Failed to clear ORG_GRADLE_PROJECT_environmentProperty',
							System.getenv('ORG_GRADLE_PROJECT_environmentProperty'))
			assertNull('Failed to clear ORG_GRADLE_PROJECT_systemProperty',
							System.getenv('ORG_GRADLE_PROJECT_systemProperty'))
			assertNull('Failed to clear ORG_GRADLE_PROJECT_commandProperty',
							System.getenv('ORG_GRADLE_PROJECT_commandProperty'))

		}
		if ( setSystemProperties ) {
			System.setProperty('org.gradle.project.systemProperty', 'System.systemValue')
			System.setProperty('org.gradle.project.commandProperty', 'System.commandValue')
		} else {
			System.clearProperty('org.gradle.project.systemProperty')
			System.clearProperty('org.gradle.project.commandProperty')
		}

		// Apply command properties.  This means 2 things:
		// 1 - Add each property to Gradle's list of command line arguments so that
		//     the plugin will process them when the plugin is applied.
		// 2 - Add the property to the project's existing properties since Gradle
		//     would have already added them to the project before applying the
		//     plugin.
		if ( commandProperties != null ) {
			commandProperties.each { key, value ->
				parentProject
				parentCommandProperties[key] = value
				parentProject.ext[key] = value
				childCommandProperties[key] = value
				childProject.ext[key] = value
			}
		}
	}

	/**
	 * Typically, a plugin is applied by name, but Gradle supports applying by
	 * type.  Prove that it works.  We don't need to get fancy here, the default
	 * environment works fine.  We only need to check that one property was read
	 * from the local file.
	 */
	public void testApplyPluginByType() {
		parentProject.apply plugin: net.saliman.gradle.plugin.properties.PropertiesPlugin
		assertEquals('local', parentProject.environmentName)
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, but we don't specify a value for that variable.
	 * The plugin should assume the project directory.  We'll try to confuse the
	 * plugin by setting the standard environmentFileDir property to a different
	 * value to make sure the plugin is using the right property.  We don't need
	 * to check every property from the file - we already know that works.
	 */
	public void testApplyChangedPropFileDirPropertyNoValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myDir
		// -PenvironmentFileDir=gradle-properties" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						environmentFileDir: 'gradle-properties' // this is a good dir.
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		//the old environmentFileDir property should still be set ...
		assertEquals('gradle-properties', parentProject.environmentFileDir)
		// ... but the property values should come from the local file in the
		// project directory.
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, and we specify a value for that variable that
	 * doesn't exist.  The plugin should fail, even if the "environmentFileDir"
	 * is still set to a valid directory.
	 */
	public void testApplyChangedPropFileDirPropertyBadValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myEnvironment
		// -PenvironmentFileDir=dummy -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						myDir: 'dummy',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're using an invalid property file directory.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, and we specify a value for that variable that
	 * points to a file.  The plugin should fail, even if the
	 * "environmentFileDir" is still set to a valid directory.
	 */
	public void testApplyChangedPropFileDirPropertyFileValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myEnvironment
		// -PenvironmentFileDir=dummy -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						myDir: 'gradle-local.properties',
						environmentFileDir: 'gradle-properties'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're using an invalid property file directory.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * property file directory, and we specify a value for that variable that
	 * points to a valid directory.  The plugin should succeed, even if
	 * "environmentFileDir" is invalid.
	 */
	public void testApplyChangedEnvironmentFileDirPropertyGoodValue() {
		// simulate a "-PpropertiesPluginEnvironmentFileDirProperty=myDir
		// -PmyDir=test -PenvironmentFileDir=dummy" command line
		def commandArgs = [
						propertiesPluginEnvironmentFileDirProperty: 'myDir',
						myDir: 'gradle-properties',
						environmentFileDir: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		// the plugin should be using the subdirectory to load properties,...
		assertEquals('gradle-properties', parentProject.myDir)
		// ... the environmentFileDir should still be set ...
		assertEquals('dummy', parentProject.environmentFileDir)
		// ... but the property values should come from the file in the subdirectory.
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentSubLocal.parentEnvironmentValue', tokens['parent.environment.property'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * environment name, but we don't specify a value for that variable.  The
	 * plugin should assume "local".  We'll try to confuse the plugin by setting
	 * the standard environment property to a different value to make sure the
	 * plugin is using the right property.  We don't need to check every property
	 * from the file - we already know that works.
	 */
	public void testApplyChangedEnvNamePropertyNoValue() {
		// simulate a "-PpropertiesPluginEnvironmentNameProperty=myEnvironment
		// -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentNameProperty: 'myEnvironment',
						environmentName: 'test'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		// the plugin should be using the local environment,...
		assertEquals('local', parentProject.myEnvironment)
		// ... the environmentName should still be set ...
		assertEquals('test', parentProject.environmentName)
		// ... but the property values should come from the local file.
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentLocal.parentEnvironmentValue', tokens['parent.environment.property'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * environment name, and we specify a value for that variable that doesn't
	 * have a file.  The plugin should fail, even if the "environmentName" is
	 * still set to a valid file.
	 */
	public void testApplyChangedEnvNamePropertyBadValue() {
		// simulate a "-PpropertiesPluginEnvironmentNameProperty=myEnvironment
		// -PmyEnvironment=dummy -PenvironmentName=test" command line
		def commandArgs = [
						propertiesPluginEnvironmentNameProperty: 'myEnvironment',
						myEnvironment: 'dummy',
						environmentName: 'test'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing an environment file.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * environment name, and we specify a value for that variable that does have
	 * have a file.  The plugin should succeed, even if "envornmentName" is
	 * invalid.
	 */
	public void testApplyChangedEnvNamePropertyGoodValue() {
		// simulate a "-PpropertiesPluginEnvironmentNameProperty=myEnvironment
		// -PmyEnvironment=test -PenvironmentName=dummy" command line
		def commandArgs = [
						propertiesPluginEnvironmentNameProperty: 'myEnvironment',
						myEnvironment: 'test',
						environmentName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		// the plugin should be using the test environment,...
		assertEquals('test', parentProject.myEnvironment)
		// ... the environmentName should still be set ...
		assertEquals('dummy', parentProject.environmentName)
		// ... but the property values should come from the local file.
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', parentProject.parentEnvironmentProperty)

		// camel case notation
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parentEnvironmentProperty'])
		// dot notation
		assertEquals('ParentEnvironmentTest.parentEnvironmentValue', tokens['parent.environment.property'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * user name, but we don't specify a value for that variable.  The
	 * plugin should assume no value, and we should get the "user" property from
	 * the "home" file.  We'll try to confuse the plugin by setting the standard
	 * user property to a different value to make sure the plugin is using the
	 * right property.  We don't need to check every property from the file - we
	 * already know that works.
	 */
	public void testApplyChangedUserNamePropertyNoValue() {
		// simulate a "-PpropertiesPluginGradleUserNameProperty=myUSer
		// -PgradleUserName=dummy" command line
		def commandArgs = [
						propertiesPluginGradleUserNameProperty: 'myUser',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens
		// the plugin should be ignoring users,...
		assertFalse(parentProject.hasProperty('myUser'))
		// ... the gradleUserName should still be set ...
		assertEquals('dummy', parentProject.gradleUserName)
		// ... but the plugin shouldn't be using it.
		assertEquals('Home.userValue', parentProject.userProperty)

		// camel case notation
		assertEquals('Home.userValue', tokens['userProperty'])
		// dot notation
		assertEquals('Home.userValue', tokens['user.property'])
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * user name, and we specify a value for that variable that doesn't have a
	 * file.  The plugin should fail, even if the "gradleUserName" is still set
	 * to a valid file.
	 */
	public void testApplyChangedUserNamePropertyBadValue() {
		// simulate a "-PpropertiesPluginGradleUserNameProperty=myUser
		// -PmyUser=dummy -PgradleUserName=user" command line
		def commandArgs = [
						propertiesPluginGradleUserNameProperty: 'myUser',
						myUser: 'dummy',
						gradleUserName: 'user'
		]
		setNonFileProperties(true, true, commandArgs)

		try {
			parentProject.apply plugin: 'properties'
			fail("We should have gotten an error when we're missing user file.")
		} catch ( Exception e) {
			// this was expected.
		}
	}

	/**
	 * Try applying the plugin when we specify a different property for the
	 * user name, and we specify a value for that variable that does have
	 * have a file.  The plugin should succeed, even if "gradleUserNAme" is
	 * invalid.
	 */
	public void testApplyChangedUserNamePropertyGoodValue() {
		// simulate a "-PpropertiesPluginGradleUserNameProperty=myUser
		// -PmyUser=user -PgradleUserName=dummy" command line
		def commandArgs = [
						propertiesPluginGradleUserNameProperty: 'myUser',
						myUser: 'user',
						gradleUserName: 'dummy'
		]
		setNonFileProperties(true, true, commandArgs)

		parentProject.apply plugin: 'properties'
		def tokens = parentProject.filterTokens

		// the plugin should be using the 'user' user,...
		assertEquals('user', parentProject.myUser)
		// ... the gradleUserName should still be set ...
		assertEquals('dummy', parentProject.gradleUserName)
		// ... but the property values should come from the User file.
		assertEquals('User.userValue', parentProject.userProperty)

		// camel case notation
		assertEquals('User.userValue', tokens['userProperty'])
		// dot notation
		assertEquals('User.userValue', tokens['user.property'])
	}
}
