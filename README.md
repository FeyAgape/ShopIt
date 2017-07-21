# ShopIt
An Inventory App: an app to track a store's inventory.

The goal is to design and create the structure of an Inventory App which would allow a store to keep track of its inventory of products. 
The app will need to store information about price, quantity available, supplier, and a picture of the product. 
It will also need to allow the user to track sales and shipments and make it easy for the user to order more from the listed supplier.


This project is about combining various ideas and skills we’ve been practicing throughout the course. They include:

Storing information in a SQLite database

Integrating Android’s file storage systems into that database

Presenting information from files and SQLite databases to users

Updating information based on user input.

Creating intents to other apps using stored information.

Overall Layout: The app contains a list of current products and a button to add a new product.

List Item Layout: Each list item displays the product name, current quantity, and price. Each list item also contains a Sale Button that reduces the quantity by one (include logic so that no negative quantities are displayed).

Detail Layout: The Detail Layout for each item displays the remainder of the information stored in the database. The Detail Layout contains buttons that increase and decrease the available quantity displayed.


The Detail Layout contains a button to order from the supplier.

The detail view contains a button to delete the product record entirely.

Default Textview: When there is no information to display in the database, the layout displays a TextView with instructions on how to populate the database.

Functionality: The code runs without errors. For example, when user inputs product information (quantity, price, name, image), instead of erroring out, the app includes logic to validate that no null values are accepted. If a null value is inputted, add a Toast that prompts the user to input the correct information before they can continue.

ListView Population: The listView populates with the current products stored in the table.

Add product button: The Add product button prompts the user for information about the product and a picture, each of which are then properly stored in the table.

Input validation: User input is validated. In particular, empty product information is not accepted. If user inputs product information (quantity, price, name, image), instead of erroring out, the app includes logic to validate that no null values are accepted. If a null value is inputted, add a Toast that prompts the user to input the correct information before they can continue.

Sale Button: Each list item contains a Sale Button which reduces the quantity available by one (include logic so that no negative quantities are displayed).

Detail View intent: Clicking on the rest of each list item sends the user to the detail screen for the correct product.

Modify quantity buttons: The modify quantity buttons in the detail view properly increase and decrease the quantity available for the correct product.

The student may also add input for how much to increase or decrease the quantity by.

Order Button: The ‘order more’ button sends an intent to either a phone app or an email app to contact the supplier using the information stored in the database.

Delete button: The delete button prompts the user for confirmation and, if confirmed, deletes the product record entirely and sends the user back to the main activity.

External Libraries and Packages: The intent of this project is to give you practice writing raw Java code using the necessary classes provided by the Android framework; therefore, the use of external libraries for core functionality will not be permitted to complete this project.
