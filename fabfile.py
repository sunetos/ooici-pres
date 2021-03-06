#!/usr/bin/env python

from __future__ import with_statement
from fabric.api import *
from fabric.contrib.console import confirm
import os
import re
import sys
import time

webAppHost = None
webAppName = None 
webAppPort = None 
def ciLogonConfig():
    global webAppName
    if webAppName is None:
        webAppName = prompt('Please enter web application name:', default='ooici-pres-0.1')

    global webAppHost
    if webAppHost is None:
        webAppHost = prompt('Please enter fully qualified web application host name:', default='grails.oceanobservatories.org')

    global webAppPort
    if webAppPort is None:
        webAppPort = prompt('Please enter web application SSL port number:', default='443')

    # Perform value substitution in CILogon config file
    o = open('web-app/WEB-INF/cfg.rdf', 'w')
    cilogonCfg = open('config/cfg.rdf.template').read()
    cilogonCfg = re.sub('APPNAME', webAppName, cilogonCfg)
    cilogonCfg = re.sub('HOSTNAME', webAppHost, cilogonCfg)
    o.write( re.sub('PORT', webAppPort, cilogonCfg) )
    o.close()

topicHost = None
topicSysname = None
topicPort = None
topicUsername = None
topicPassword = None
topicExchange = None
instrumentMonitorURL = None
debugMode = None
def appConfig(localDeployment):
    global topicHost
    if topicHost is None:
        if localDeployment:
            topicHost = prompt('Please enter fully qualified topic host name:', default='localhost')
        else:
            topicHost = prompt('Please enter fully qualified topic host name:', default='amoeba.ucsd.edu')

    global topicSysname
    if topicSysname is None:
        if localDeployment:
            topicSysname = prompt('Please enter topic sysname:', default=os.getlogin())
        else:
            topicSysname = prompt('Please enter topic sysname:', default='R1_UI_DEMO')

    global topicPort
    if topicPort is None:
        topicPort = prompt('Please enter topic AMQ port number:', default='5672')

    global topicUsername
    if topicUsername is None:
        topicUsername = prompt('Please enter username for connecting to RabbitMQ:', default='guest')

    global topicPassword
    if topicPassword is None:
        topicPassword = prompt('Please enter password for connecting to RabbitMQ:', default='guest')

    global topicExchange
    if topicExchange is None:
        topicExchange = prompt('Please enter topic exchange:', default='magnet.topic')

    global instrumentMonitorURL
    if instrumentMonitorURL is None:
        if localDeployment:
            instrumentMonitorURL = prompt('Please enter instrument monitor URL:', default='http://localhost:9998')
        else:
            instrumentMonitorURL = prompt('Please enter instrument monitor URL:', default='http://pubdebug01.oceanobservatories.org:9998')

    global debugMode
    if debugMode is None:
        if localDeployment:
            debugMode = prompt('Please enter CILogon bypass authentication mode (force, url, disabled):', default='force')
        else:
            debugMode = prompt('Please enter CILogon bypass authentication mode (force, url, disabled):', default='disabled')
    
    # Perform value substitution in app config file
    if localDeployment:
        homeDir = os.environ['HOME']
        o = open('%s/.grails/ioncore-config.properties'% (homeDir), 'w')
    else:
        o = open('src/java/ioncore-config.properties', 'w')
    appCfg = open('config/ioncore-config.properties.template').read()
    appCfg = re.sub('HOSTNAME', topicHost, appCfg)
    appCfg = re.sub('SYSNAME', topicSysname, appCfg)
    appCfg = re.sub('PORT', topicPort, appCfg)
    appCfg = re.sub('USERNAME', topicUsername, appCfg)
    appCfg = re.sub('PASSWORD', topicPassword, appCfg)
    appCfg = re.sub('INSTRUMENT_MONITOR_URL', instrumentMonitorURL, appCfg)
    appCfg = re.sub('BYPASSAUTHENTICATIONMODE', debugMode, appCfg)
    o.write( re.sub('EXCHANGE', topicExchange, appCfg) )
    o.close()

def buildWebApp(localDeployment):
    if not localDeployment:
        ciLogonConfig()
    appConfig(localDeployment)

    # Build and package app
    os.system('grails war')

sshUser = None
def startWebApp(localDeployment):
    if localDeployment:
        local('grails run-app')
    else:
        global webAppHost
        if webAppHost is None:
            webAppHost = prompt('Please enter fully qualified web application host name:', default='grails.oceanobservatories.org')
        global sshUser
        if sshUser is None:
            sshUser = os.getlogin()
            sshUser = prompt('Please enter your ssh login name:', default=sshUser)

        local('ssh %s@%s -t sudo /etc/init.d/grails stop' % (sshUser, webAppHost))
        print 'Waiting for application to fully stop'
        time.sleep(10);
        local('ssh %s@%s -t sudo rm -rf /opt/tomcat/webapps/%s*' % (sshUser, webAppHost, webAppName))
        local('scp target/%s.war %s@%s:/opt/tomcat/webapps' % (webAppName, sshUser, webAppHost))
        local('ssh %s@%s -t sudo /etc/init.d/grails start' % (sshUser, webAppHost))

def deployRemote():
    buildWebApp(False)
    startWebApp(False)

def deployLocal():
    buildWebApp(True)
    startWebApp(True)
