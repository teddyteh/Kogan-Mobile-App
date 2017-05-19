package com.teddyteh.kmusage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

/**
 * Created by teddy on 15/5/2017.
 */

public class MainActivityPresenterTest {
    MainPresenter presenter;

    @Mock
    MainView view;

    @Before
    public void setUp() {
        presenter = new MainPresenterImpl(view);
    }

    @Test
    public void test() {
        presenter.populateSpinner();
    }
}
