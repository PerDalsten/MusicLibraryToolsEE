WLP:

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


 Wildfly:
 
 Install ActiveMQ RA as a module (or deploy as application).
 
 <subsystem xmlns="urn:jboss:domain:resource-adapters:5.0">
            <resource-adapters>
                <resource-adapter id="activemq">
                    <module slot="main" id="activemq.ra" />
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
        
        
Glassfish:
 
Install RA: 
  
~/Development/glassfish4/bin$ ./asadmin deploy --type rar --name activemq ~/Downloads/activemq-rar-5.15.3.rar 

~/Development/glassfish4/bin$ ./asadmin create-resource-adapter-config  --property ServerUrl='tcp\://localhost\:61616' activemq  
  
App specific:
  
~/Development/glassfish4/bin$ ./asadmin create-connector-connection-pool --raname activemq --connectiondefinition javax.jms.ConnectionFactory --ping true --isconnectvalidatereq true jms/ActiveMQConnectionPool

~/Development/glassfish4/bin$ ./asadmin create-connector-resource --poolname jms/ActiveMQConnectionPool jms/ActiveMQConnectionFactory

~/Development/glassfish4/bin$ ./asadmin create-admin-object --raname activemq --restype javax.jms.Queue --property PhysicalName=MUSICLIBRARY.SONG jms/SongQueue
        