# Wavee - Simple android loading indicator written entirely in Kotlin

To add it to your project:

In your root build.gradle add this

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
in your apps build.gradle add this:

	dependencies {
	        implementation 'com.github.LukasAnda:Wavee:v1.0'
	}

and that is it!