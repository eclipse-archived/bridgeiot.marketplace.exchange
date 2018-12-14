# Usage

The generation and validation of license headers is not integrated in the sbt build. 
Because of better feature support, a gradle build script is used instead, that is apart from the rest of the (sbt-based) build. 
In the gradle build script the com.github.hierynomus.license plugin is used that offers excellent support especially for the validation.
For example, in non-strict mode it is possible to extend the license header with author attributions while it is still recognized to be valid.
Sbt license plugins do not offer such a non-strict mode.

As the license build does not make use of the gradle-wrapper, Gradle has to be installed on the system and available in the system PATH variable.
The gradle.build file is located in a separate license-gen directory to show that it is only used for the purpose of license generation and validation.
Hence, to run the license plugin, you have to switch from project root directory to license-gen directory:

     cd license-gen
     
Then run gradle license task to verify license headers:

     gradle license
    
For generation of license headers, please run:

     gradle licenseFormat