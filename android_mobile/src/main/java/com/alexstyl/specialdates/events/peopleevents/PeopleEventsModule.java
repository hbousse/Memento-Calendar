package com.alexstyl.specialdates.events.peopleevents;

import android.appwidget.AppWidgetManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

import com.alexstyl.specialdates.contact.ContactsProvider;
import com.alexstyl.specialdates.events.PeopleEventsMonitor;
import com.alexstyl.specialdates.events.database.EventSQLiteOpenHelper;
import com.alexstyl.specialdates.events.namedays.NamedayDatabaseRefresher;
import com.alexstyl.specialdates.events.namedays.NamedayUserSettings;
import com.alexstyl.specialdates.events.namedays.calendar.resource.NamedayCalendarProvider;
import com.alexstyl.specialdates.upcoming.widget.list.UpcomingEventsScrollingWidgetView;
import com.alexstyl.specialdates.upcoming.widget.today.TodayPeopleEventsView;
import com.alexstyl.specialdates.util.DateParser;
import com.alexstyl.specialdates.wear.WearSyncPeopleEventsView;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashSet;

import dagger.Module;
import dagger.Provides;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Module
@Singleton
public class PeopleEventsModule {

    private final Context context;

    public PeopleEventsModule(Context context) {
        this.context = context;
    }

    @Provides
    PeopleStaticEventsProvider peopleStaticEventsProvider(ContactsProvider contactsProvider) {
        return new AndroidPeopleStaticEventsProvider(
                new EventSQLiteOpenHelper(context),
                contactsProvider,
                new CustomEventProvider(context.getContentResolver())
        );
    }

    @Provides
    PeopleEventsProvider peopleEventsProvider(NamedayUserSettings namedayUserSettings,
                                              PeopleNamedaysCalculator peopleNamedaysCalculator,
                                              PeopleStaticEventsProvider staticEvents) {
        return new PeopleEventsProvider(namedayUserSettings, peopleNamedaysCalculator, staticEvents);
    }

    @Provides
    PeopleNamedaysCalculator peopleNamedayCalculator(NamedayUserSettings namedayPreferences,
                                                     NamedayCalendarProvider namedaysCalendarProvider,
                                                     ContactsProvider contactsProvider) {
        return new PeopleNamedaysCalculator(namedayPreferences, namedaysCalendarProvider, contactsProvider);
    }

    @Provides
    @Singleton
    PeopleEventsViewRefresher peopleEventsViewRefresher(Context appContext, AppWidgetManager appWidgetManager) {
        return new PeopleEventsViewRefresher(new HashSet<>(Arrays.asList(
                new WearSyncPeopleEventsView(appContext),
                new TodayPeopleEventsView(appContext, appWidgetManager),
                new UpcomingEventsScrollingWidgetView(appContext, appWidgetManager)
        )));
    }

    @Provides
    PeopleEventsStaticEventsRefresher peopleEventsStaticEventsRefresher(SQLiteOpenHelper eventSQlite, ContentResolver contentResolver, ContactsProvider contactsProvider) {
        AndroidEventsRepository repository = new AndroidEventsRepository(contentResolver, contactsProvider, DateParser.INSTANCE);
        ContactEventsMarshaller marshaller = new ContactEventsMarshaller();
        PeopleEventsPersister peopleEventsPersister = new PeopleEventsPersister(eventSQlite);
        return new PeopleEventsStaticEventsRefresher(repository, marshaller, peopleEventsPersister);
    }

    @Provides
    NamedayDatabaseRefresher namedayDatabaseRefresher(NamedayUserSettings namedayUserSettings,
                                                      PeopleEventsPersister databaseProvider,
                                                      PeopleNamedaysCalculator calculator) {
        return new NamedayDatabaseRefresher(namedayUserSettings, databaseProvider, new ContactEventsMarshaller(), calculator);
    }

    @Provides
    PeopleEventsUpdater peopleEventsUpdater(PeopleEventsStaticEventsRefresher staticRefresher, NamedayDatabaseRefresher namedayRefresher) {
        return new PeopleEventsUpdater(staticRefresher, namedayRefresher);
    }

    @Provides
    @Singleton
    PeopleEventsMonitor peopleEventsDatabaseUpdater(PeopleEventsViewRefresher uiRefresher, PeopleEventsUpdater peopleEventsUpdater) {
        return new PeopleEventsMonitor(peopleEventsUpdater, uiRefresher, Schedulers.io(), AndroidSchedulers.mainThread());
    }

    @Provides
    PeopleEventsPersister peopleEventsPersister() {
        return new PeopleEventsPersister(new EventSQLiteOpenHelper(context));
    }

    @Provides
    EventPreferences eventPreferences() {
        return new EventPreferences(context);
    }
}
