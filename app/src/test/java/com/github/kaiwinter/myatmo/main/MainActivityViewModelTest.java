package com.github.kaiwinter.myatmo.main;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;
import com.github.kaiwinter.myatmo.util.SingleLiveEvent;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MainActivityViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantExecutorRule = new InstantTaskExecutorRule();

    private MockWebServer mockWebServer = new MockWebServer();

    /**
     * The access token is missing when data should be loaded. The app should navigate to the LoginActivity.
     */
    @Test
    public void switchToMainActivity() {
        SharedPreferencesStore sharedPreferences =
                when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("").getMock();
        MainActivityViewModel viewModel = new MainActivityViewModel(null, sharedPreferences, null, null);

        viewModel.navigateToLoginActivity = mock(SingleLiveEvent.class);
        viewModel.getdata();

        verify(viewModel.navigateToLoginActivity).call();
    }

    @Test
    public void refreshAccessToken() throws IOException {
        InputStream resourceAsStream = MainActivityViewModelTest.class.getResourceAsStream("/stationsdata_200.txt");
        Buffer buffer = new Buffer().readFrom(resourceAsStream);
        mockWebServer.enqueue(new MockResponse().setBody(buffer));
        mockWebServer.start(8080);
        SharedPreferencesStore sharedPreferences = mock(SharedPreferencesStore.class);

        when(sharedPreferences.getAccessToken()).thenReturn("ABC");
        AccessTokenManager tokenManager = mock(AccessTokenManager.class);
        StationsDataService service = ServiceGenerator.createService(StationsDataService.class);

        MainActivityViewModel viewModel = new MainActivityViewModel(null, sharedPreferences, tokenManager, service);
        viewModel.getdata();

        await().atMost(20, TimeUnit.SECONDS).until(() -> viewModel.outdoorModule.getValue() != null);

        ModuleVO outdoor = viewModel.outdoorModule.getValue();
        assertEquals("Schlafzimmer", outdoor.moduleName);
        assertEquals(ModuleVO.ModuleType.OUTDOOR, outdoor.moduleType);
        assertEquals(1615837300, outdoor.beginTime.longValue());
        assertEquals(56, outdoor.humidity.longValue());
        assertEquals(20.3, outdoor.temperature, 0);
        assertEquals("B_ID", outdoor.id);

        ModuleVO indoor = viewModel.indoorModule.getValue();
        assertEquals("Wohnzimmer", indoor.moduleName);
        assertEquals(ModuleVO.ModuleType.INDOOR, indoor.moduleType);
        assertEquals(1615837309, indoor.beginTime.longValue());
        assertEquals(1087, indoor.co2.longValue());
        assertEquals(49, indoor.humidity.longValue());
        assertEquals(21.5, indoor.temperature, 0);
    }
}