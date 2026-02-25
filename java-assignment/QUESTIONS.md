# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
The warehouse seems to have best overall clean design approach which closely follows a Hexagonal Architecture. If the code base were to increase then functional domain will prove to be easier change and refactor implementations in each layer, whilst minimizing the impact of other layers. Hence if there was a future descision to change the from the Quarkus framework to SpringBoot then could be achieved without changing any code in the domain layer. 

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
Open API yaml:
Pros:
The project requires Strict API contract which decided quite earlier in the product lifecycle. 
The API has multiple dependant REST client consumers such as end UI clients, subsystems like Microservice consumers, large scale development project with multiple teams. Public goverment APIs for government IT systems and central banking systems which require numerous standard and secure API integration. 
Code generation appears to be clean, and the relatively simple boiler plate code is readable and hence increases code productivety as it handles DTO creation and request validation.

Cons: 
May not be a appropriate for scenarios where there is a need to quickly  protoype solutions for the technical proof of concepts.
When the project is in the early stages of the development lifecycle and the interfaces /APIs are not clearly defined or agreed.
```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
I would focus the most complex and riskiest implementations, which in this case appears to be the warehouse business function, especially the replace warehouse  usecase.
how would you ensure test coverage remains effective over time? This can achieved by using code coverage tools such as Jacaoco, which can included in the CI/CD pipeline build process. The code coverage 'jacoco.minimum.line.coverage' percentage setting can be adjusted (i.e. increased) in order to ensure that test code coverage is improved by the development team. 
```