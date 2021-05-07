package com.github.kaiwinter.myatmo.main;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.kaiwinter.myatmo.login.AccessTokenManager;
import com.github.kaiwinter.myatmo.main.rest.StationsDataService;
import com.github.kaiwinter.myatmo.storage.SharedPreferencesStore;
import com.github.kaiwinter.myatmo.util.SingleLiveEvent;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

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

    /**
     * The access token is missing when data should be loaded. The app should navigate to the LoginActivity.
     */
    @Test
    public void getdata_notLoggedIn_switchToMainActivity() {
        SharedPreferencesStore sharedPreferences = when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("").getMock();
        MainActivityViewModel viewModel = new MainActivityViewModel(null, sharedPreferences, null, null);

        viewModel.navigateToLoginActivity = mock(SingleLiveEvent.class);
        viewModel.getdata();

        verify(viewModel.navigateToLoginActivity).call();
    }

    /**
     * Receives a valid JSON from the mock server and check if the viewmodel is filled accordingly.
     */
    @Test
    public void getdata_valid() throws IOException {

        runWithMockWebServer("/stationsdata_200.json", 200, () -> {

            SharedPreferencesStore sharedPreferences = when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("ABC").getMock();
            AccessTokenManager tokenManager = mock(AccessTokenManager.class);
            StationsDataService service = ServiceGenerator.createService(StationsDataService.class);

            MainActivityViewModel viewModel = new MainActivityViewModel(null, sharedPreferences, tokenManager, service);
            viewModel.getdata();

            await().atMost(10, TimeUnit.SECONDS).until(() -> viewModel.outdoorModule.getValue() != null);

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
        });
    }

    @Test
    public void getdata_401() throws IOException {
        runWithMockWebServer("/401_2.json", 401, () -> {
            SharedPreferencesStore sharedPreferences = when(mock(SharedPreferencesStore.class).getAccessToken()).thenReturn("ABC").getMock();
            AccessTokenManager tokenManager = mock(AccessTokenManager.class);
            StationsDataService service = ServiceGenerator.createService(StationsDataService.class);

            MainActivityViewModel viewModel = new MainActivityViewModel(null, sharedPreferences, tokenManager, service);

            viewModel.navigateToRelogin = mock(SingleLiveEvent.class);
            viewModel.getdata();

            await().untilAsserted(() -> viewModel.navigateToRelogin.postValue(ArgumentMatchers.eq("401: Invalid token missing (2)")));
        });
    }

    private void runWithMockWebServer(String file, int httpCode, Runnable runnable) throws IOException {
        InputStream inputStream = MainActivityViewModelTest.class.getResourceAsStream(file);
        Buffer buffer = new Buffer().readFrom(inputStream);

        MockWebServer mockWebServer = new MockWebServer();
        mockWebServer.enqueue(new MockResponse().setBody(buffer).setResponseCode(httpCode));
        mockWebServer.start(8080);
        runnable.run();
        mockWebServer.close();
    }
}