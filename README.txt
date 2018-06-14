Application server versions
===========================

Websphere Liberty 17.0.0.4
Wildfly 11.0.0.Final
Glassfish 4.1.2
TomEE Plus 7.0.4


General configuration
=====================

Configure datasources and logging as described in MusicLibrary.

Install ActiveMQ (5.15.3)


WLP Server config
=================

Install (if using web profile instead of full WLP, and not already added): 

./installUtility install jms-2.0
./installUtility install mdb-3.2
./installUtility install wasJmsClient-2.0
./installUtility install jaxws-2.2 
	
mkdir lib/ActiveMQ
cp /path/to/activemq-rar-5.15.3.rar lib/ActiveMQ/

server.xml:

    <featureManager>
        <feature>webProfile-7.0</feature>
        <feature>localConnector-1.0</feature>        
        <feature>jaxws-2.2</feature>
        <feature>jms-2.0</feature>
        <feature>mdb-3.2</feature>
        <feature>wasJmsClient-2.0</feature>	
    </featureManager>

     <resourceAdapter id="activemq" location="${server.config.dir}/lib/ActiveMQ/activemq-rar-5.15.3.rar">
         <properties.activemq serverUrl="tcp://localhost:61616"/>
     </resourceAdapter>
     
     <jmsQueueConnectionFactory jndiName="jms/ActiveMQConnectionFactory">
        <properties.activemq/>
     </jmsQueueConnectionFactory>
    
     <jmsQueue id="MUSICLIBRARY.SONG" jndiName="jms/SongQueue">
          <properties.activemq PhysicalName="MUSICLIBRARY.SONG"/>
     </jmsQueue>

     <jmsActivationSpec id="MusicLibraryToolsEE/SongReceiver">
          <properties.activemq/>
     </jmsActivationSpec>



Wildfly config
==============

Install ActiveMQ RA as a module.

mkdir  -p modules/activemq/ra/main/
cd modules/activemq/ra/main/
cp /path/to/activemq-rar-5.15.3.rar .
unzip activemq-rar-5.15.3.rar
rm *.rar

Create module.xml:

Include all jars, replace with ones already in Wildfly and exclude potentially conflicting ones from export.

<?xml version='1.0' encoding='UTF-8'?>
<module xmlns="urn:jboss:module:1.1" name="activemq.ra">
    <resources>
      <resource-root path="."/>
      <resource-root path="activemq-amqp-5.15.3.jar"/>
      <resource-root path="activemq-broker-5.15.3.jar"/>
      <resource-root path="activemq-client-5.15.3.jar"/>
      <resource-root path="activemq-jms-pool-5.15.3.jar"/>
      <resource-root path="activemq-kahadb-store-5.15.3.jar"/>
      <resource-root path="activemq-mqtt-5.15.3.jar"/>
      <resource-root path="activemq-openwire-legacy-5.15.3.jar"/>
      <resource-root path="activemq-pool-5.15.3.jar"/>
      <resource-root path="activemq-protobuf-1.1.jar"/>
      <resource-root path="activemq-ra-5.15.3.jar"/>
      <resource-root path="activemq-spring-5.15.3.jar"/>
      <resource-root path="commons-logging-1.2.jar"/>
      <resource-root path="commons-net-3.6.jar"/>
      <resource-root path="commons-pool2-2.4.2.jar"/>
      <resource-root path="geronimo-j2ee-management_1.1_spec-1.0.1.jar"/>
      <resource-root path="guava-18.0.jar"/>
      <resource-root path="hawtbuf-1.11.jar"/>
      <resource-root path="hawtdispatch-1.22.jar"/>
      <resource-root path="hawtdispatch-transport-1.22.jar"/>
      <resource-root path="jackson-annotations-2.6.7.jar"/>
      <resource-root path="jackson-core-2.6.7.jar"/>
      <resource-root path="jackson-databind-2.6.7.jar"/>
      <resource-root path="mqtt-client-1.14.jar"/>
      <resource-root path="proton-j-0.25.0.jar"/>
      <resource-root path="spring-aop-4.3.9.RELEASE.jar"/>
      <resource-root path="spring-beans-4.3.9.RELEASE.jar"/>
      <resource-root path="spring-context-4.3.9.RELEASE.jar"/>
      <resource-root path="spring-core-4.3.9.RELEASE.jar"/>
      <resource-root path="spring-expression-4.3.9.RELEASE.jar"/>
      <resource-root path="xbean-spring-4.2.jar"/>
    </resources>

    <exports>
      <exclude path="org/springframework/**"/>
      <exclude path="org/apache/xbean/**"/>
      <exclude path="org/apache/commons/**"/>
      <exclude path="com/google/**"/>
      <exclude path="org/fusesource/**"/>
      <exclude path="com/fasterxml/jackson/**"/>
    </exports>

    <dependencies>
      <module name="javax.api"/>
      <module name="org.slf4j"/>
      <module name="javax.resource.api"/>
      <module name="javax.jms.api"/>
      <module name="javax.management.j2ee.api"/>
    </dependencies>
</module>


As an alternative the entire rar can be deployed as an appplication (copied to standalone/deployments), in which case
refer to archive rather than module below.

 
Modify standalone.xml:

       <subsystem xmlns="urn:jboss:domain:ejb3:5.0">
           ...
            </session-bean>
            <mdb>
                <resource-adapter-ref resource-adapter-name="activemq"/>
            </mdb>
            <pools>
          ...
 
        <subsystem xmlns="urn:jboss:domain:resource-adapters:5.0">
            <resource-adapters>
                <resource-adapter id="activemq">
                    <module slot="main" id="activemq.ra" />
                    <!--
                    <archive>
                        activemq-rar-5.15.3.rar
                    </archive>
                     -->
                    <transaction-support>XATransaction</transaction-support>
                    <config-property name="ServerUrl">
                        tcp://localhost:61616
                    </config-property>
                    <connection-definitions>                        
                        <connection-definition class-name="org.apache.activemq.ra.ActiveMQManagedConnectionFactory" jndi-name="java:/jms/ActiveMQConnectionFactory" enabled="true" pool-name="ActiveMQConnectionFactory">
                            <xa-pool>
                                <min-pool-size>1</min-pool-size>
                                <max-pool-size>20</max-pool-size>
                                <prefill>false</prefill>
                                <is-same-rm-override>false</is-same-rm-override>
                            </xa-pool>
                        </connection-definition>
                    </connection-definitions>
                    <admin-objects>
                        <admin-object class-name="org.apache.activemq.command.ActiveMQQueue" jndi-name="java:/jms/SongQueue" use-java-context="true" pool-name="SongQueue">
                            <config-property name="PhysicalName">
                                MUSICLIBRARY.SONG
                            </config-property>
                        </admin-object>
                    </admin-objects>
                </resource-adapter>
            </resource-adapters>
        </subsystem>    
        
        
Glassfish config
================
 
Configure ActiveMQ: 
  
./asadmin deploy --type rar --name activemq /path/to/activemq-rar-5.15.3.rar 

./asadmin create-resource-adapter-config  --property ServerUrl='tcp\://localhost\:61616' activemq  
  
./asadmin create-connector-connection-pool --raname activemq --connectiondefinition javax.jms.ConnectionFactory --ping true --isconnectvalidatereq true jms/ActiveMQConnectionPool

./asadmin create-connector-resource --poolname jms/ActiveMQConnectionPool jms/ActiveMQConnectionFactory

./asadmin create-admin-object --raname activemq --restype javax.jms.Queue --property PhysicalName=MUSICLIBRARY.SONG jms/SongQueue


TomEE config
============

Add to conf/tomee.xml

<tomee>
  <Resource id="activemq" type="ActiveMQResourceAdapter">
      BrokerXmlConfig =
      ServerUrl       =  tcp://localhost:61616
  </Resource>

  <Resource id="jms/ActiveMQConnectionFactory" type="javax.jms.ConnectionFactory">
      ResourceAdapter = activemq
  </Resource>

  <Resource id="jms/SongQueue" type="javax.jms.Queue">
      destination = MUSICLIBRARY.SONG
  </Resource>    

  <Container id="activemqMDB" ctype="MESSAGE">
      ResourceAdapter = activemq
  </Container>
</tomee>
        