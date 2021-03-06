package com.alexstyl.specialdates.events.peopleevents;

import android.content.Context;

import com.alexstyl.specialdates.contact.ContactsProvider;
import com.alexstyl.specialdates.events.database.EventSQLiteOpenHelper;
import com.alexstyl.specialdates.events.namedays.NamedayDatabaseRefresher;
import com.alexstyl.specialdates.events.namedays.NamedayUserSettings;
import com.alexstyl.specialdates.events.namedays.calendar.resource.NamedayCalendarProvider;
import com.alexstyl.specialdates.util.DateParser;

public final class DebugPeopleEventsUpdater {

    private final PeopleEventsStaticEventsRefresher peopleEventsStaticEventsRefresher;
    private final NamedayDatabaseRefresher namedayDatabaseRefresher;

    public static DebugPeopleEventsUpdater newInstance(Context context, NamedayUserSettings namedayUserSettings, ContactsProvider contactsProvider) {
        AndroidEventsRepository repository = new AndroidEventsRepository(context.getContentResolver(), contactsProvider, DateParser.INSTANCE);
        ContactEventsMarshaller deviceMarshaller = new ContactEventsMarshaller();
        PeopleEventsPersister databaseProvider = new PeopleEventsPersister(new EventSQLiteOpenHelper(context));

        PeopleEventsStaticEventsRefresher databaseRefresher = new PeopleEventsStaticEventsRefresher(repository, deviceMarshaller, databaseProvider);

        NamedayCalendarProvider namedayCalendarProvider = NamedayCalendarProvider.newInstance(context.getResources());
        PeopleNamedaysCalculator peopleNamedaysCalculator = new PeopleNamedaysCalculator(
                namedayUserSettings,
                namedayCalendarProvider,
                contactsProvider
        );
        return new DebugPeopleEventsUpdater(databaseRefresher, new NamedayDatabaseRefresher(
                namedayUserSettings,
                databaseProvider,
                deviceMarshaller,
                peopleNamedaysCalculator
        ));
    }

    private DebugPeopleEventsUpdater(PeopleEventsStaticEventsRefresher peopleEventsStaticEventsRefresher, NamedayDatabaseRefresher namedayDatabaseRefresher) {
        this.peopleEventsStaticEventsRefresher = peopleEventsStaticEventsRefresher;
        this.namedayDatabaseRefresher = namedayDatabaseRefresher;
    }

    public void refresh() {
        peopleEventsStaticEventsRefresher.rebuildEvents();
        namedayDatabaseRefresher.refreshNamedaysIfEnabled();
    }
}
