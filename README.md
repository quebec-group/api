[![Build Status](https://travis-ci.org/quebec-group/api.svg?branch=master)](https://travis-ci.org/quebec-group/api)
# api 
Contains code for the AWS lambda functions that run the backend of this project.

## Setup
Set the following environment variables for the db connection:
 - dbUrl
 - dbUser
 - dbPassword

## Documentation
Documentation about the calls available to the app are provided here http://docs.quebec1.apiary.io/#

## Lambda functions
This code base provides the following
 - APIHandler - Handles all requests coming from the app
 - EventProcessedHandler - Called when the face detection is complete
 - ProfileProcessedHandler - Called when face training is complete

## Deployment
Running the assemble gradle task will build a zip file that can be uploaded directly into AWS Lambda. 
Currently Travis will upload this to S3, where another Lambda function will update the code for the relevant functions.
