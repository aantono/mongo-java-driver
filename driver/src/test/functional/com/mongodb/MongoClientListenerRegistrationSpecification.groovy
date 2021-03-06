/*
 * Copyright 2015 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb

import com.mongodb.event.ClusterListener
import com.mongodb.event.CommandListener
import com.mongodb.event.ConnectionListener
import com.mongodb.event.ConnectionPoolListener
import com.mongodb.event.ServerListener
import com.mongodb.event.ServerMonitorListener
import org.bson.Document

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static com.mongodb.Fixture.mongoClientURI

class MongoClientListenerRegistrationSpecification extends FunctionalSpecification {

    def 'should register single command listener'() {
        given:
        def first = Mock(CommandListener)
        def client = new MongoClient(mongoClientURI.getHosts().collect { new ServerAddress(it) },
                MongoClientOptions.builder(mongoClientURI.options)
                                                       .addCommandListener(first)
                                                       .build());

        when:
        client.getDatabase('admin').runCommand(new Document('ping', 1))

        then:
        1 * first.commandStarted(_)
        1 * first.commandSucceeded(_)
    }

    def 'should register multiple command listeners'() {
        given:
        def first = Mock(CommandListener)
        def second = Mock(CommandListener)
        def client = new MongoClient(mongoClientURI.getHosts().collect { new ServerAddress(it) },
                                     MongoClientOptions.builder(mongoClientURI.options)
                                                       .addCommandListener(first)
                                                       .addCommandListener(second).build());

        when:
        client.getDatabase('admin').runCommand(new Document('ping', 1))

        then:
        1 * first.commandStarted(_)
        1 * second.commandStarted(_)
        1 * first.commandSucceeded(_)
        1 * second.commandSucceeded(_)
    }

    def 'should register single listeners for monitor events'() {
        given:
        def latch = new CountDownLatch(1)
        def clusterListener = Mock(ClusterListener) {
           1 * clusterOpening(_)
        }
        def serverListener = Mock(ServerListener) {
            (1.._) * serverOpening(_)
        }
        def serverMonitorListener = Mock(ServerMonitorListener){
            (1.._) * serverHearbeatStarted(_) >> {
                if (latch.count > 0) {
                    latch.countDown()
                }
            }
        }
        def connectionPoolListener = Mock(ConnectionPoolListener){
            (1.._) * connectionPoolOpened(_)
        }
        def connectionListener = Mock(ConnectionListener){
            (1.._) * connectionOpened(_)
        }

        def client = new MongoClient(mongoClientURI.getHosts().collect { new ServerAddress(it) },
                MongoClientOptions.builder(mongoClientURI.options)
                        .addClusterListener(clusterListener)
                        .addServerListener(serverListener)
                        .addServerMonitorListener(serverMonitorListener)
                        .addConnectionPoolListener(connectionPoolListener)
                        .addConnectionListener(connectionListener)
                        .build());

        when:
        def finished = latch.await(5, TimeUnit.SECONDS)

        then:
        finished

        cleanup:
        client?.close()
    }

    def 'should register multiple listeners for monitor events'() {
        given:
        def latch = new CountDownLatch(2)
        def clusterListener = Mock(ClusterListener) {
            1 * clusterOpening(_)
        }
        def serverListener = Mock(ServerListener) {
            (1.._) * serverOpening(_)
        }
        def serverMonitorListener = Mock(ServerMonitorListener){
            (1.._) * serverHearbeatStarted(_) >> {
                if (latch.count > 0) {
                    latch.countDown()
                }
            }
        }
        def connectionPoolListener = Mock(ConnectionPoolListener){
            (1.._) * connectionPoolOpened(_)
        }
        def connectionListener = Mock(ConnectionListener){
            (1.._) * connectionOpened(_)
        }
        def clusterListenerTwo = Mock(ClusterListener) {
            1 * clusterOpening(_)
        }
        def serverListenerTwo = Mock(ServerListener) {
            (1.._) * serverOpening(_)
        }
        def serverMonitorListenerTwo = Mock(ServerMonitorListener){
            (1.._) * serverHearbeatStarted(_) >> {
                if (latch.count > 0) {
                    latch.countDown()
                }
            }
        }
        def connectionPoolListenerTwo = Mock(ConnectionPoolListener){
            (1.._) * connectionPoolOpened(_)
        }
        def connectionListenerTwo = Mock(ConnectionListener){
            (1.._) * connectionOpened(_)
        }

        def client = new MongoClient(mongoClientURI.getHosts().collect { new ServerAddress(it) },
                MongoClientOptions.builder(mongoClientURI.options)
                        .addClusterListener(clusterListener)
                        .addServerListener(serverListener)
                        .addServerMonitorListener(serverMonitorListener)
                        .addConnectionPoolListener(connectionPoolListener)
                        .addConnectionListener(connectionListener)
                        .addClusterListener(clusterListenerTwo)
                        .addServerListener(serverListenerTwo)
                        .addServerMonitorListener(serverMonitorListenerTwo)
                        .addConnectionPoolListener(connectionPoolListenerTwo)
                        .addConnectionListener(connectionListenerTwo)
                        .build());

        when:
        def finished = latch.await(5, TimeUnit.SECONDS)

        then:
        finished

        cleanup:
        client?.close()
    }
}
