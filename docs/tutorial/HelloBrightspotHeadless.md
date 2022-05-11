# Hello Brightspot Headless with GraphQL, React & Typescript

This tutorial will guide you through the steps to create a Brightspot application and React/Typescript application that communicate with each other over a Brightspot powered GraphQL API.

**Before you begin, make sure you have checked out the `feature/graphql-react-typescript` branch of this repository.**

Refer to the README at the root of the project for [running the Brightspot application with Docker](/README.md). Refer to the README in the grapqhl/react-typescript folder for [running the React application with Typescript](/graphql/react-typescript/README.md).

## Step 1: Launch a Site in Brightspot

If you haven't done so already, deploy the vanilla Brightspot application from a terminal using the provided docker scripts and navigate to the CMS at `http://localhost/cms`. **NOTE:** You only need to run `docker/initialize` one time. If you already have, just run `docker/deploy`.

```
docker/initialize
docker/deploy
```

Log in to the CMS using any credentials that you'd like. From the burger menu in the top left corner, navigate to `Admin → Sites & Settings`. Click the `New Site` link at the top to be presented with the Site creation form.

Name your site by filling out the `Name` field with something like "React App". A little further down you'll see the `Urls` field. Configure it to point to the URL of our React application which will run at `http://localhost:3000`. Add a second URL that points to the Brightspot application at `http://localhost`. The first URL allows Brightspot to link off to the React application when appropriate while also accepting traffic from the second URL and associating it with our Site. Click `Save` to create the Site and then return to the dashboard.

| Login | Dashboard | Create Site | Save Site |
| --- | --- | --- | --- |
| ![step01a](https://user-images.githubusercontent.com/361297/107783682-8d2b4d00-6d18-11eb-8fce-d094e18664e2.png) | ![step01b](https://user-images.githubusercontent.com/361297/107783683-8d2b4d00-6d18-11eb-8af7-d1f832ff03f5.png) | ![step01c](https://user-images.githubusercontent.com/361297/107783684-8d2b4d00-6d18-11eb-9cc1-fe262ef9d5ba.png) | ![step01d](https://user-images.githubusercontent.com/361297/107783686-8d2b4d00-6d18-11eb-8fa8-b6376277ba7d.png) |

## Step 2: Create a Content Type

Content types in Brightspot are represented by Java classes that extend the class `Content`. The instance variables added to the class represent the fields of your content type.

We'll begin by creating a Content Type called `HelloBrightspot` with a single `name` field. The Java files should go in the `src/main/java/` directory in the `com/brightspot/tutorial` package.

```java
package com.brightspot.tutorial;

import com.psddev.cms.db.Content;

public class HelloBrightspot extends Content {

    private String name;
}
```

Refresh your browser window to see the result. Your newly created content type should appear in the "All Content Types" dropdown in the left rail of the Search UI when you click into the Search bar in the header. Selecting `Hello Brightspot` will yield no matching items, but you can create new instances by clicking the `New` button in the bottom left corner. You'll be presented with an edit page containing the single field `name` that we defined.

**NOTE:** If your content type doesn't appear you can force a reload by appending the parameter `?_reload=true` to your browser address bar and hit Enter to reload the app.

| Search | New Content |
| --- | --- |
| ![step02a](https://user-images.githubusercontent.com/361297/107783687-8dc3e380-6d18-11eb-97a7-2c6ae5a5f414.png) | ![step02b](https://user-images.githubusercontent.com/361297/107783689-8dc3e380-6d18-11eb-808e-2cc724b61924.png) |

Before publishing any instances though, let's spruce up our data model a bit. First, we'll add getters and setters since we'll eventually want to access the data in our field. And then we'll also define permalink generation rules by implementing the `Directory.Item` interface's `String createPermalink(Site)` method to provide logic for deriving a permalink path from the content type's data. The implementation below prepends the String `/hello-` to the normalized (URL safe) name field when it's present.

```java
package com.brightspot.tutorial;

import java.util.Optional;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Site;
import com.psddev.dari.util.Utils;
import org.apache.commons.lang3.StringUtils;

public class HelloBrightspot extends Content implements Directory.Item {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String createPermalink(Site site) {
        return Optional.ofNullable(getName())
            .map(Utils::toNormalized)
            .filter(StringUtils::isNotEmpty)
            .map("/hello-"::concat)
            .orElse(null);
    }
}
```

Refresh your browser window, and you should see the Brightspot Reloader screen appear. Give it a few seconds to reload and once back on the edit form, go ahead and fill out a name. You'll notice the URLs widget in the right rail will automatically update to reflect our permalink generation rules. Feel free to publish the content and verify that it appears in search results and the recent activity widget on the dashboard.

| Dashboard Quick Start | Create Content | Publish Content | Search Content
| --- | --- | --- | --- 
| ![step02c](https://user-images.githubusercontent.com/361297/107783691-8dc3e380-6d18-11eb-8b2b-e366f5098582.png) | ![step02d](https://user-images.githubusercontent.com/361297/107783693-8dc3e380-6d18-11eb-82fe-fc5f584cdb40.png) | ![step02e](https://user-images.githubusercontent.com/361297/107783694-8dc3e380-6d18-11eb-933b-2c0de29c70ce.png) | ![step02f](https://user-images.githubusercontent.com/361297/107783695-8e5c7a00-6d18-11eb-8ab2-34c57a3b988c.png) ||

## Step 3: Create a View Model

Next, we'll create a View Model, which serves two primary functions: 1) Defines the type schema for our GraphQL API, and 2) Contains business logic for transforming our raw content type into a format better suited for downstream consumption.

Create a new class named `HelloBrightspotViewModel` as shown below with a single method `String getMessage()`. The generic argument in `ViewModel<HelloBrightspot>` is the same type as the instance variable `model`, allowing us to call our `getName()` getter method to access our `name` field. The `getMessage()` implementation prepends the word "Hello " to the `name` field, or defaults to "Brightspot" if `name` is missing. We also add some Javadocs which will surface in our generated GraphQL schema to make our API more developer friendly.

```java
package com.brightspot.tutorial;

import java.util.Optional;

import com.psddev.cms.view.PageEntryView;
import com.psddev.cms.view.ViewInterface;
import com.psddev.cms.view.ViewModel;

/**
 * A warm Brightspot welcome.
 */
@ViewInterface
public class HelloBrightspotViewModel extends ViewModel<HelloBrightspot> implements PageEntryView {

    /**
     * A friendly welcome message.
     */
    public String getMessage() {
        return "Hello " + Optional.ofNullable(model.getName())
            .orElse("Brightspot");
    }
}
```

You can verify the creation of your `ViewModel` by navigating to `http://localhost/_debug/` and clicking the `View Interface: Schema Viewer` link under the `Standard Tools` heading. Clicking into the Select box should yield a single result of `HelloBrightspotViewModel`. Select it, and click the "View" button to reveal our `ViewModel` as having a single text field named `message`.

![step03](https://user-images.githubusercontent.com/361297/107787396-06c53a00-6d1d-11eb-9589-4799e624ebcb.png)

## Step 4: Define a GraphQL API Endpoint

At this step we could create the endpoint editorially without any code at all, however as a best practice we're going to define it in code so that we don't have to republish it every time our code gets deployed to a new environment. Our endpoint will be persistent, consistent and versioned under source control so that we can safely reference it later from our React App.

Create a new class called `HelloBrightspotApi`. We extend `ContentDeliveryApiEndpoint` to define it as a GraphQL Content Delivery API Endpoint. It implements `Singleton` to signify that only one instance of our endpoint exists (which is true for most APIs). The `#getPathSuffix()` method declares a unique path in which we can access our endpoint. `#getQueryEntryFields()` declares the list of `ViewModels` we want to include in our API, in this case just `HelloBrightspotViewModel`. Our `#getAccessOption()` implementation makes the API public to the world, without requiring any auth (there are other options that can restrict access and require API keys, etc.). Finally, the `#updateCorsConfiguration(GraphQLCorsConfiguration)` method ensures that requests from `localhost` (on any port) are allowed to access our API cross origin, enabling our React App to communicate with Brightspot.

```java
package com.brightspot.tutorial;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.psddev.dari.db.Singleton;
import com.psddev.graphql.GraphQLCorsConfiguration;
import com.psddev.graphql.cda.ContentDeliveryApiAccessOption;
import com.psddev.graphql.cda.ContentDeliveryApiAccessOptionImplicit;
import com.psddev.graphql.cda.ContentDeliveryApiEndpoint;
import com.psddev.graphql.cda.ContentDeliveryEntryPointField;

public class HelloBrightspotApi extends ContentDeliveryApiEndpoint implements Singleton {

    @Override
    protected String getPathSuffix() {
        return "/hello-brightspot";
    }

    @Override
    public List<ContentDeliveryEntryPointField> getQueryEntryFields() {
        return Stream.of(
            new ContentDeliveryEntryPointField(
                HelloBrightspotViewModel.class, // The backing ViewModel
                "HelloBrightspot", // The GraphQL query field name
                "Say hello to Brightspot.") // The GraphQL query field description
        ).collect(Collectors.toList());
    }

    @Override
    public ContentDeliveryApiAccessOption getAccessOption() {
        return new ContentDeliveryApiAccessOptionImplicit();
    }

    @Override
    protected void updateCorsConfiguration(GraphQLCorsConfiguration corsConfiguration) {
        super.updateCorsConfiguration(corsConfiguration);
        corsConfiguration.addWhitelistedDomain("localhost");
    }
}
```

From the burger menu in the top left corner, navigate to `Admin → APIs`. You'll see your newly created `Hello Brightspot API`. Clicking on it will reveal the full path your endpoint (e.g. `/graphql/delivery/hello-brightspot`).

Now, let's see the API in action! From the burger menu, navigate to `Developer → GraphQL`, and select `Hello Brightspot API` from the `Select GraphQL Endpoint...` dropdown. You'll be presented with the [GraphiQL Explorer](https://github.com/OneGraph/graphiql-explorer) UI with your GraphQL schema loaded into it. In the Explorer pane in the left rail, you'll see a single GraphQL query field named `HelloBrightspot` that accepts either an `id` or a `path` and returns a `message`. Similarly, in the Documentation Explorer in the right rail, you can navigate through the Query type and see all the fields and types available in the API. Note that the Javadocs we added in the previous step appear here as part of the schema docs! If you haven't already, publish some `Hello Brightspot` content so that you can search for it in the GraphQL Explorer UI. The content's permalink (e.g. `/hello-<name>`) can be used in the `path` field.

To get us queued up for the next step, we're going to create a query that contains variables so that we can alter the data we pass to the API without having to alter the query. Either by hand or by using the GraphiQL Explorer interface to help you, construct a query that looks like the following:

```graphql
query HelloBrightspot($path: String, $id: ID) {
  HelloBrightspot(id: $id, path: $path) {
    message
  }
}
```

In the `Query Variables` input form right below add a JSON structure with `path` as a key, and a value equal to a known permalink of your published content and verify that you still get back results. Ex:

```json
{
  "path": "/hello-<name>"
}
```

If that worked, you are ready to configure and run your React application.

| API Endpoint | GraphQL Explorer |
| --- | --- |
| ![step04a](https://user-images.githubusercontent.com/361297/107783700-8e5c7a00-6d18-11eb-96c9-b527c17c6fc2.png) | ![step04b](https://user-images.githubusercontent.com/361297/107783701-8e5c7a00-6d18-11eb-9c2e-d9d28a5a04dd.png) |

## Step 5: Configure and Run the React App

The React/Typescript app, found inside of the `graphql/react-typescript` directory, was bootstrapped with [Create React App](https://github.com/facebook/create-react-app), and then further modified to add [GraphQL tooling](https://codeburst.io/tooling-your-typescript-create-react-app-to-work-with-graphql-d845e035d44e). We will use [yarn](https://yarnpkg.com/lang/en/) for managing the app. You can read more info on how to interact with the available scripts at the [README here](/graphql/react-typescript/README.md).

Open a terminal and navigate to the `graphql/react-typescript` directory of the project. We'll start off by downloading and installing all the necessary dependencies to run the application by running the `yarn` command (no arguments necessary). Next, let's start the application with `yarn start`. Upon successful start up, your default browser should open to `http://localhost:3000` and you'll see the React logo.

Congrats! Now let's get to work making Brightspot and React communicate with each other!

You can leave the app running for now. In an IDE or new terminal window, from the same `graphql/react-typescript` directory, edit the `codegen.yml` file. Line 2 contains a `schema` field. Enter the full URL to the Brightspot GraphQL endpoint (e.g. `http://localhost/graphql/delivery/hello-brightspot`) between the quotes, then save and close the file.

Next, open and edit `src/index.tsx`. Line 14 contains a `HttpLink` uri. Enter the same Brightspot GraphQL endpoint URL from the previous step between those quotes, then save and close the file.

Now, let's build some components!

Create a new directory structure in the `graphql/react-typescript/src` folder named `components/HelloBrightspot`. Inside there, create a file named `queries.ts` where we will create a new Apollo client `gql` constant with the query from our GraphQL Explorer UI from the last step. The file should look like this:

```typescript
import { gql } from '@apollo/client';

export const GET_HELLO_BRIGHTSPOT = gql`
    query HelloBrightspot($id: ID, $path: String) {
        HelloBrightspot(id: $id, path: $path) {
            message
        }
    }
`;
```

The name of the `const` doesn't really matter as the GraphQL codegen will generate a variable based on the `query` name which in our case we have named it `HelloBrightspot`.

In the same `components/HelloBrightspot` folder create a new file called `index.tsx`. Here is where we'll define our HelloBrightspot component. We import our `HelloBrightspot` query as a function named `useHelloBrightspotQuery` that was auto-generated in the `generated/graphql.tsx` file. This function takes care of calling the Brightspot GraphQL API and returning the results. In our example we pass the `path` as a variable with whatever pathname is in the browser, so if you navigate to `http://localhost:3000/hello-<name>` it will pass `hello-<name>` as the `path`, which we tested in the previous step. From there we add some simple loading and error logic, and finally fetch the `HelloBrightspot/message` value from the `data` response and return it in a `<h1>` component. Note that because we are using Typescript and because the query and response components were auto-generated based off the GraphQL schema, we get both code auto-complete and compile time checking that we are fetching the correct data fields. If the schema ever changes our code will automatically be updated to reflect that and give us compile errors if things do not match! 

```typescript jsx
import React from "react";
import { useHelloBrightspotQuery } from "../../generated/graphql";

const HelloBrightspot = () => {
    const VARIABLES = { path: window.location.pathname }

    const { data, loading, error } = useHelloBrightspotQuery({
        variables: VARIABLES,
    });

    if (loading) {
        return <div>Loading...</div>;
    }

    if (error) {
        return <div>{error.message}</div>;
    }

    if (!data?.HelloBrightspot) {
        return <div>404</div>;
    }

    return (
        <h1>{data?.HelloBrightspot?.message}</h1>
    );
};

export default HelloBrightspot;
```

The final step here is to use our new `HelloBrightspot` component in the main `App` component. Edit the `src/App.tsx` file and import our component. Then replace everything inside of the `<header className="App-header">` element with the `<HelloBrightspot />` component. The final file should look like this: 

```typescript jsx
import React from 'react';
import './App.css';
import HelloBrightspot from "./components/HelloBrightspot";

function App() {
  return (
    <div className="App">
      <header className="App-header">
        <HelloBrightspot />
      </header>
    </div>
  );
}

export default App;
```

Put it all together now by going back to Brightspot in your browser and either publish a new or edit an existing `Hello Brightspot` content type. The URLs widget will display the permalink path, and when clicked, will take you to your React application and display the results! Success! Congrats! 🎉

| Create React App | React App Powered by Brightspot GraphQL API |
| --- | --- |
| ![step05a](https://user-images.githubusercontent.com/361297/107783702-8e5c7a00-6d18-11eb-80d7-231c7691824e.png) | ![step05b](https://user-images.githubusercontent.com/361297/107783703-8ef51080-6d18-11eb-8c02-7a2fac13bfc0.png) |

## Step 6 (Extra Credit): Enable Live Preview

With just a little more code in our React app, and some configuration on the Brightspot side we can enable a live preview of our app from inside of Brightspot!

Go back to our `HelloBrightspot` component in `src/components/HelloBrightspot/index.tsx`. We're going to change the way the `VARIABLES` map gets calculated by first looking for a special `previewId` parameter that we'll receive from Brightspot, and pass it to the `id` variable instead of passing `path`. The updated section of code looks like the following:

```typescript jsx
const urlParams = new URLSearchParams(window.location.search)
const previewId = urlParams.get('previewId')
const VARIABLES = previewId != null ? { id: previewId } : { path: window.location.pathname }
```

Back in the Brightspot UI, from the burger menu, navigate to `Admin → Sites & Settings`. Your `React App` Site should be pre-selected. Fom the `Main` Tab look for the `Preview` Cluster down below and expand it. In the `Preview Types` field click `Add GraphQL Preview`. In the new form enter a value in the `Name` field like, "React App", and the `Preview URL` field should be pre-populated with `http://localhost:3000` which points to our React app. Finally, in the `Default Preview Type` field, select `React App` from the dropdown. Save your changes and go back to the dashboard.

Now, create a new `Hello Brightspot` content type using the Quick Start widget on the dashboard. The Preview pane should automatically be open and render a live preview of our application. As you type in the `Name` field, the Preview should reflect the changes in real-time! 🎊 🎉 🎊 🎉


| Enable GraphQL Preview | Live Preview In Action |
| --- | --- |
| ![step06a](https://user-images.githubusercontent.com/361297/107816835-54ed3400-6d43-11eb-942e-84391734c0db.png) | ![step06b](https://user-images.githubusercontent.com/361297/107783704-8ef51080-6d18-11eb-8fb5-0b3c38f86506.png) |