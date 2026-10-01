# BluePOS Go Client Demo 

This test project intended to show how BluePOS Go SDK library could be used 
in external application

The process of using BluePOS Go SDK

See documentation provided with BluePOS Go SDK

## Local payment configuration

Payment credentials are intentionally not committed to Git. Create a local
`payment.properties` file in the project root:

```properties
BLUEPOS_ACCOUNT_ID=your-account-id
BLUEPOS_API_KEY=your-api-key
BLUEPOS_API_SECRET=your-api-secret
BLUEPOS_ENVIRONMENT=STAGING
```

You can also provide the same names as Gradle properties or environment
variables.
